package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.infrastructure.persistence.repository.VariableExpenseJpaRepository
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

class VariableExpenseIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var expenses: VariableExpenseJpaRepository

    @Test
    fun should_return_created_expense_with_location_when_posting() {
        val user = createUser()

        val result = postJson(user, VARIABLE, """{"name":" Netflix ","monthlyAmount":42000,"category":"STREAMING"}""")
        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Netflix") }
            jsonPath("$.data.category") { value("STREAMING") }
            content { string(containsString("\"monthlyAmount\":42000.00")) }
        }
        val id = idOf(result)
        assertEquals("http://localhost$VARIABLE/$id", result.andReturn().response.getHeader(HttpHeaders.LOCATION))
    }

    @Test
    fun should_replace_fields_when_updating_and_soft_delete_when_deleting() {
        val user = createUser()
        val id = createExpense(user, "Netflix", "STREAMING", "42000")

        putJson(user, "$VARIABLE/$id", """{"name":"Doña Gladys","monthlyAmount":360000,"category":"HOUSEHOLD_STAFF"}""")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.category") { value("HOUSEHOLD_STAFF") }
            }
        assertEquals("HOUSEHOLD_STAFF", expenses.findById(id).orElseThrow().category)
        deleteAs(user, "$VARIABLE/$id").andExpect { status { isNoContent() } }
        assertFalse(expenses.findById(id).orElseThrow().active)
    }

    @Test
    fun should_return_validation_error_when_category_belongs_to_fixed_expenses() {
        postJson(createUser(), VARIABLE, """{"name":"Mercado","monthlyAmount":1,"category":"FOOD"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.details[0].field") { value("category") }
        }
    }

    @Test
    fun should_sort_by_category_then_name_and_sum_only_own_active_expenses_when_listing_and_summarizing() {
        val user = createUser()
        createExpense(user, "Ocio Yas", "LEISURE", "1000000")
        createExpense(user, "Netflix", "STREAMING", "42000")
        createExpense(user, "ocio Casa", "LEISURE", "500000")
        createExpense(user, "Doña Gladys", "HOUSEHOLD_STAFF", "360000")
        deleteAs(user, "$VARIABLE/${createExpense(user, "Viejo", "GIFTS", "999")}")
        createExpense(createUser(), "Ajeno", "LEISURE", "5")

        mockMvc.get(VARIABLE) { bearer(user) }.andExpect {
            jsonPath("$.data[*].name") { value(contains("ocio Casa", "Ocio Yas", "Netflix", "Doña Gladys")) }
        }
        mockMvc.get("$VARIABLE?category=LEISURE") { bearer(user) }.andExpect {
            jsonPath("$.data[*].name") { value(contains("ocio Casa", "Ocio Yas")) }
        }
        mockMvc.get("$VARIABLE/summary") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString(EXPECTED_SUMMARY)) }
        }
    }

    private fun createExpense(user: User, name: String, category: String, amount: String): UUID =
        idOf(postJson(user, VARIABLE, """{"name":"$name","monthlyAmount":$amount,"category":"$category"}"""))

    private companion object {
        const val VARIABLE = "/api/v1/variable-expenses"
        const val EXPECTED_SUMMARY =
            """{"totalMonthlyAmount":1902000.00,"count":4,"byCategory":[""" +
                """{"category":"LEISURE","totalMonthlyAmount":1500000.00,"count":2},""" +
                """{"category":"STREAMING","totalMonthlyAmount":42000.00,"count":1},""" +
                """{"category":"HOUSEHOLD_STAFF","totalMonthlyAmount":360000.00,"count":1}]}"""
    }
}
