package com.pennypincher.pro.budget.infrastructure.web.controller

import com.pennypincher.pro.budget.infrastructure.persistence.adapter.BudgetSourceRows
import com.pennypincher.pro.budget.infrastructure.persistence.repository.MonthlyBudgetJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class MonthlyBudgetIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var budgets: MonthlyBudgetJpaRepository

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun should_create_budget_with_figures_from_active_records() {
        val user = createUser()
        seedSpreadsheetMonth(user)

        val result = postJson(user, BUDGETS, """{"year":2026,"month":10,"exchangeRate":3300,"workDays":22}""")
        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.year") { value(2026) }
            jsonPath("$.data.month") { value(10) }
            jsonPath("$.data.workDays") { value(22) }
            jsonPath("$.data.components[*].component") {
                value(contains("CREDIT_CARDS", "LOANS", "MONTHLY_EXPENSES", "SAVINGS", "CHARITY"))
            }
            jsonPath("$.data.userId") { doesNotExist() }
            content {
                string(containsString("\"exchangeRate\":3300.00"))
                string(containsString("\"totalIncome\":37152922.36"))
                string(containsString("\"totalExpenses\":19135201.17"))
                string(containsString("\"monthlyCashFlow\":18017721.19"))
                string(containsString("\"creditCardPercentage\":0.00"))
                string(containsString("\"loanPercentage\":8.83"))
                string(containsString("\"expensePercentage\":42.67"))
                string(containsString("\"savingsPercentage\":0.00"))
                string(containsString("\"charityPercentage\":0.00"))
                string(containsString(component("CREDIT_CARDS", "0.00", "0.00", "20.00")))
                string(containsString(component("LOANS", "3280231.00", "8.83", "null")))
                string(containsString(component("MONTHLY_EXPENSES", "15854970.17", "42.67", "null")))
                string(containsString(component("SAVINGS", "0.00", "0.00", "10.00")))
                string(containsString(component("CHARITY", "0.00", "0.00", "10.00")))
            }
        }
    }

    @Test
    fun should_answer_location_of_the_month_and_store_component_totals_when_creating() {
        val user = createUser()
        seedSpreadsheetMonth(user)

        val result = postJson(user, BUDGETS, """{"year":2026,"month":10}""")

        val location = checkNotNull(result.andReturn().response.getHeader(HttpHeaders.LOCATION))
        assertEquals("http://localhost$BUDGETS/2026/10", location)
        mockMvc.get(location) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(idOf(result).toString()) }
            content { string(containsString(component("LOANS", "3280231.00", "8.83", "null"))) }
        }
        val stored = budgets.findById(idOf(result)).orElseThrow()
        assertEquals("15854970.17", stored.components.expenseTotal.toPlainString())
        assertEquals("42.6749", stored.components.expensePercentage.toPlainString())
    }

    @Test
    fun should_return_conflict_when_month_already_has_a_budget_and_allow_it_for_another_user() {
        val user = createUser()
        postJson(user, BUDGETS, """{"year":2026,"month":10}""").andExpect { status { isCreated() } }

        postJson(user, BUDGETS, """{"year":2026,"month":10,"workDays":20}""").andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("CONFLICT") }
            jsonPath("$.error.message") { value("A budget for 2026-10 already exists") }
        }
        postJson(createUser(), BUDGETS, """{"year":2026,"month":10}""").andExpect { status { isCreated() } }
    }

    @Test
    fun should_create_future_month_with_null_settings_and_zero_figures() {
        val user = createUser()

        postJson(user, BUDGETS, """{"year":2099,"month":1}""").andExpect {
            status { isCreated() }
            jsonPath("$.data.exchangeRate") { value(null as Any?) }
            jsonPath("$.data.workDays") { value(null as Any?) }
            content {
                string(containsString("\"exchangeRate\":null"))
                string(containsString("\"workDays\":null"))
                string(containsString("\"totalIncome\":0.00"))
                string(containsString("\"monthlyCashFlow\":0.00"))
            }
        }
    }

    @Test
    fun should_list_own_budgets_newest_first_and_filter_by_year() {
        val user = createUser()
        listOf(2025 to 12, 2026 to 2, 2026 to 11, 2024 to 1).forEach { (year, month) ->
            postJson(user, BUDGETS, """{"year":$year,"month":$month}""")
        }
        postJson(createUser(), BUDGETS, """{"year":2026,"month":5}""")

        mockMvc.get(BUDGETS) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].year") { value(contains(2026, 2026, 2025, 2024)) }
            jsonPath("$.data[*].month") { value(contains(11, 2, 12, 1)) }
        }
        mockMvc.get("$BUDGETS?year=2026") { bearer(user) }.andExpect {
            jsonPath("$.data[*].month") { value(contains(11, 2)) }
        }
        mockMvc.get("$BUDGETS?year=2030") { bearer(user) }.andExpect { jsonPath("$.data") { isEmpty() } }
    }

    @Test
    fun should_replace_settings_and_recalculate_when_updating() {
        val user = createUser()
        val id = idOf(postJson(user, BUDGETS, """{"year":2026,"month":10,"exchangeRate":3300,"workDays":22}"""))
        createIncome(user, "2000")
        BudgetSourceRows(jdbc).creditCard(user.id, "500")

        putJson(user, "$BUDGETS/$id", """{"exchangeRate":4100.5}""").andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(id.toString()) }
            jsonPath("$.data.workDays") { value(null as Any?) }
            content {
                string(containsString("\"exchangeRate\":4100.50"))
                string(containsString("\"totalIncome\":2000.00"))
                string(containsString("\"totalExpenses\":500.00"))
                string(containsString("\"creditCardPercentage\":25.00"))
            }
        }
        val stored = budgets.findById(id).orElseThrow()
        assertEquals(null, stored.workDays)
        assertEquals("500.00", stored.components.creditCardTotal.toPlainString())
    }

    @Test
    fun should_recalculate_from_current_records_with_charity_split_and_negative_cash_flow() {
        val user = createUser()
        createIncome(user, "1000")
        val id = idOf(postJson(user, BUDGETS, """{"year":2026,"month":9,"workDays":20}"""))
        val rows = BudgetSourceRows(jdbc)
        rows.fixedExpense(user.id, "900", "HOUSING")
        rows.fixedExpense(user.id, "100", "CHARITY")
        rows.variableExpense(user.id, "200")
        rows.savings(user.id, "50")

        mockMvc.post("$BUDGETS/$id/recalculate") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.workDays") { value(20) }
            content {
                string(containsString("\"totalExpenses\":1200.00"))
                string(containsString("\"monthlyCashFlow\":-200.00"))
                string(containsString("\"expensePercentage\":110.00"))
                string(containsString("\"charityPercentage\":10.00"))
                string(containsString("\"savingsPercentage\":5.00"))
            }
        }
        assertEquals("-200.00", budgets.findById(id).orElseThrow().monthlyCashFlow.toPlainString())
    }

    private fun seedSpreadsheetMonth(user: User) {
        createIncome(user, "15878607.36")
        createIncome(user, "21274315.00")
        deleteAs(user, "/api/v1/income-sources/${createIncome(user, "999")}")
        val rows = BudgetSourceRows(jdbc)
        rows.loan(user.id, "3280231")
        rows.fixedExpense(user.id, "15000000")
        rows.variableExpense(user.id, "854970.17")
        rows.creditCard(user.id, "999", active = false)
        rows.creditCard(createUser().id, "999")
    }

    private fun createIncome(user: User, amount: String): UUID = idOf(
        postJson(user, "/api/v1/income-sources", """{"name":"Ingreso","category":"EMPLOYMENT","amount":$amount}"""),
    )

    private fun component(key: String, amount: String, percentage: String, ideal: String) =
        """{"component":"$key","amount":$amount,"percentage":$percentage,"idealPercentage":$ideal}"""

    private companion object {
        const val BUDGETS = "/api/v1/monthly-budgets"
    }
}
