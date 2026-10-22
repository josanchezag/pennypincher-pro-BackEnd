package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.infrastructure.persistence.repository.FixedExpenseJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.get

class FixedExpenseIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var expenses: FixedExpenseJpaRepository

    @Test
    fun should_return_created_expense_with_location_and_two_decimals_when_posting() {
        val user = createUser()

        val result =
            postJson(user, FIXED, """{"name":" Colegio Thiana ","monthlyAmount":2271042,"category":"EDUCATION"}""")
        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Colegio Thiana") }
            jsonPath("$.data.category") { value("EDUCATION") }
            jsonPath("$.data.userId") { doesNotExist() }
            content { string(containsString("\"monthlyAmount\":2271042.00")) }
        }
        val id = idOf(result)
        assertEquals("http://localhost$FIXED/$id", result.andReturn().response.getHeader(HttpHeaders.LOCATION))
        val stored = expenses.findById(id).orElseThrow()
        assertEquals("EDUCATION", stored.category)
        assertEquals("2271042.00", stored.amount.toPlainString())
    }

    @Test
    fun should_list_own_active_expenses_by_category_then_name_when_listing_with_or_without_filter() {
        val user = createUser()
        createExpense(user, "Natación Thiana", "EDUCATION")
        createExpense(user, "Retencion en la fuente", "TAXES")
        createExpense(user, "colegio Thiana", "EDUCATION")
        createExpense(user, "Mercado", "FOOD")
        deleteAs(user, "$FIXED/${createExpense(user, "Viejo", "FOOD")}")
        createExpense(createUser(), "Ajeno", "FOOD")

        mockMvc.get(FIXED) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") {
                value(contains("Mercado", "colegio Thiana", "Natación Thiana", "Retencion en la fuente"))
            }
        }
        mockMvc.get("$FIXED?category=EDUCATION") { bearer(user) }.andExpect {
            jsonPath("$.data[*].name") { value(contains("colegio Thiana", "Natación Thiana")) }
        }
    }

    @Test
    fun should_replace_fields_when_updating_and_soft_delete_when_deleting() {
        val user = createUser()
        val id = createExpense(user, "Poliza", "OTHER")

        putJson(user, "$FIXED/$id", """{"name":"Poliza Salud","monthlyAmount":1382421,"category":"INSURANCE"}""")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(id.toString()) }
                jsonPath("$.data.category") { value("INSURANCE") }
                content { string(containsString("\"monthlyAmount\":1382421.00")) }
            }
        deleteAs(user, "$FIXED/$id").andExpect { status { isNoContent() } }
        assertFalse(expenses.findById(id).orElseThrow().active)
    }

    @Test
    fun should_group_own_active_expenses_in_enum_order_when_summarizing_even_with_filter() {
        val user = createUser()
        mockMvc.get("$FIXED/summary") { bearer(user) }.andExpect {
            content { string(containsString("""{"totalMonthlyAmount":0.00,"count":0,"byCategory":[]}""")) }
        }
        createExpense(user, "Donación Aldeas Colombia", "CHARITY", "50000")
        createExpense(user, "Colegio Thiana", "EDUCATION", "2271042")
        createExpense(user, "Natación Thiana", "EDUCATION", "76667")
        createExpense(user, "Mercado", "FOOD", "1100000")
        deleteAs(user, "$FIXED/${createExpense(user, "Viejo", "TAXES", "999")}")
        createExpense(createUser(), "Ajeno", "FOOD", "5")

        mockMvc.get("$FIXED/summary?category=FOOD") { bearer(user) }.andExpect {
            status { isOk() }
            content {
                string(
                    containsString(
                        """{"totalMonthlyAmount":3497709.00,"count":4,"byCategory":[""" +
                            """{"category":"FOOD","totalMonthlyAmount":1100000.00,"count":1},""" +
                            """{"category":"EDUCATION","totalMonthlyAmount":2347709.00,"count":2},""" +
                            """{"category":"CHARITY","totalMonthlyAmount":50000.00,"count":1}]}""",
                    ),
                )
            }
        }
    }

    private fun createExpense(user: User, name: String, category: String, amount: String = "100"): UUID =
        idOf(postJson(user, FIXED, """{"name":"$name","monthlyAmount":$amount,"category":"$category"}"""))

    private companion object {
        const val FIXED = "/api/v1/fixed-expenses"
    }
}
