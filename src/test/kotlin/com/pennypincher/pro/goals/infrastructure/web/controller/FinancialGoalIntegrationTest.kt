package com.pennypincher.pro.goals.infrastructure.web.controller

import com.pennypincher.pro.goals.infrastructure.persistence.repository.FinancialGoalJpaRepository
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class FinancialGoalIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var goals: FinancialGoalJpaRepository

    @Test
    fun should_return_created_goal_with_location_and_description_when_posting() {
        val user = createUser()

        val result = postJson(user, GOALS, body(name = "\" Seguridad Financiera \""))

        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Seguridad Financiera") }
            jsonPath("$.data.level") { value("FINANCIAL_SECURITY") }
            jsonPath("$.data.levelDescription") { value("Expenses covered for 6 months without income") }
            jsonPath("$.data.targetYears") { value(TARGET_YEARS) }
            jsonPath("$.data.userId") { doesNotExist() }
            content {
                string(containsString("\"targetAmount\":605000000.00"))
                string(containsString("\"monthlyInvestment\":1470000.00"))
                string(containsString("\"expectedAnnualReturn\":0.1100"))
            }
        }
        val id = idOf(result)
        assertEquals("http://localhost$GOALS/$id", result.andReturn().response.getHeader(HttpHeaders.LOCATION))
        mockMvc.get("$GOALS/$id") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(id.toString()) }
        }
    }

    @Test
    fun should_replace_all_fields_and_clear_target_years_when_putting_without_them() {
        val user = createUser()
        val id = createGoal(user, body())

        putJson(user, "$GOALS/$id", body(level = "ABSOLUTE_FREEDOM", years = null, annualReturn = "0.0825"))
            .andExpect {
                status { isOk() }
                jsonPath("$.data.level") { value("ABSOLUTE_FREEDOM") }
                jsonPath("$.data.levelDescription") { value("Unlimited financial choices") }
                content {
                    string(containsString("\"targetYears\":null"))
                    string(containsString("\"expectedAnnualReturn\":0.0825"))
                }
            }
        val stored = goals.findById(id).orElseThrow()
        assertEquals("ABSOLUTE_FREEDOM", stored.level)
        assertEquals(null, stored.targetYears)
    }

    @Test
    fun should_remove_the_row_when_deleting() {
        val user = createUser()
        val id = createGoal(user, body())

        deleteAs(user, "$GOALS/$id").andExpect { status { isNoContent() } }

        assertFalse(goals.existsById(id))
        deleteAs(user, "$GOALS/$id").andExpect { status { isNotFound() } }
    }

    @Test
    fun should_return_not_found_and_leave_goal_untouched_when_it_belongs_to_another_user() {
        val owner = createUser()
        val id = createGoal(owner, body())
        val intruder = createUser()
        val path = "$GOALS/$id"

        listOf(mockMvc.get(path) { bearer(intruder) }, putJson(intruder, path, body()), deleteAs(intruder, path))
            .forEach {
                it.andExpect {
                    status { isNotFound() }
                    jsonPath("$.error.code") { value("NOT_FOUND") }
                }
            }
        mockMvc.get("$GOALS/${UUID.randomUUID()}") { bearer(owner) }.andExpect { status { isNotFound() } }
        mockMvc.get(GOALS) { bearer(intruder) }.andExpect { jsonPath("$.data") { isEmpty() } }
        mockMvc.get(path) { bearer(owner) }.andExpect { status { isOk() } }
    }

    @Test
    fun should_order_by_level_then_target_then_name_when_listing() {
        val user = createUser()
        createGoal(user, body(name = "\"zeta\"", level = "ABSOLUTE_FREEDOM", target = "1"))
        createGoal(user, body(name = "\"beta\"", target = "900"))
        createGoal(user, body(name = "\"Alfa\"", target = "900"))
        createGoal(user, body(name = "\"gamma\"", target = "100"))
        createGoal(user, body(name = "\"delta\"", level = "FINANCIAL_INDEPENDENCE", target = "5"))
        createGoal(createUser(), body(name = "\"ajena\""))

        mockMvc.get(GOALS) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") { value(contains("gamma", "Alfa", "beta", "delta", "zeta")) }
        }
    }

    @Test
    fun should_report_the_field_when_body_breaks_a_rule() {
        val user = createUser()
        invalidBodies().forEach { (body, field) ->
            postJson(user, GOALS, body).andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                jsonPath("$.error.details[0].field") { value(field) }
            }
        }
        mockMvc.get(GOALS) { bearer(user) }.andExpect { jsonPath("$.data") { isEmpty() } }
    }

    @Test
    fun should_create_missing_standard_goals_once_when_requesting_defaults() {
        val user = createUser()
        createGoal(user, body(name = "\"Mi colchon\"", target = "50000000"))

        repeat(2) {
            mockMvc.post("$GOALS/defaults") { bearer(user) }.andExpect {
                status { isOk() }
                jsonPath("$.data[*].name") {
                    value(
                        contains("Mi colchon", "Independencia Financiera", "Libertad Financiera", "Libertad Absoluta"),
                    )
                }
                jsonPath("$.data[3].targetYears") { value(TARGET_YEARS) }
                content { string(containsString("\"targetAmount\":4366000000.00,\"monthlyInvestment\":15850000.00")) }
            }
        }
        assertEquals(EXPECTED_GOALS_AFTER_DEFAULTS, goals.findAllByUserId(user.id).size)
    }

    @Test
    fun should_list_levels_with_descriptions_in_display_order() {
        mockMvc.get("$GOALS/levels") { bearer(createUser()) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].level") {
                value(contains("FINANCIAL_SECURITY", "FINANCIAL_INDEPENDENCE", "FINANCIAL_FREEDOM", "ABSOLUTE_FREEDOM"))
            }
            jsonPath("$.data[1].description") { value("Passive income covers all basic expenses") }
            jsonPath("$.data[2].description") { value("Passive income covers desired lifestyle") }
        }
    }

    @Test
    fun should_require_authentication_on_every_goal_endpoint() {
        listOf(
            mockMvc.get(GOALS),
            mockMvc.get("$GOALS/levels"),
            mockMvc.get("$GOALS/${UUID.randomUUID()}"),
            mockMvc.post("$GOALS/defaults"),
            mockMvc.post(GOALS) { json(body()) },
            mockMvc.put("$GOALS/${UUID.randomUUID()}") { json(body()) },
            mockMvc.delete("$GOALS/${UUID.randomUUID()}"),
        ).forEach { it.andExpect { status { isUnauthorized() } } }
    }

    private fun invalidBodies(): List<Pair<String, String>> = listOf(
        body(name = null) to "name",
        body(name = "\"  \"") to "name",
        body(name = "\"${"x".repeat(NAME_TOO_LONG)}\"") to "name",
        body(level = null) to "level",
        body(level = "YACHT") to "level",
        body(target = null) to "targetAmount",
        body(target = "0") to "targetAmount",
        body(target = "1.001") to "targetAmount",
        body(target = "\"abc\"") to "targetAmount",
        body(monthly = null) to "monthlyInvestment",
        body(monthly = "-0.01") to "monthlyInvestment",
        body(monthly = "12345678901234567") to "monthlyInvestment",
        body(annualReturn = null) to "expectedAnnualReturn",
        body(annualReturn = "0") to "expectedAnnualReturn",
        body(annualReturn = "1.01") to "expectedAnnualReturn",
        body(annualReturn = "0.12345") to "expectedAnnualReturn",
        body(years = "0") to "targetYears",
        body(years = "101") to "targetYears",
        body(years = "1.5") to "targetYears",
        body(years = "99999999999") to "targetYears",
        body(years = "\"5\"") to "targetYears",
        body(target = "\"605000000\"") to "targetAmount",
    )

    private fun createGoal(user: User, body: String): UUID = idOf(postJson(user, GOALS, body))

    /** A valid goal body; each value is raw JSON, and a null value leaves the field out. */
    private fun body(
        name: String? = "\"Seguridad Financiera\"",
        level: String? = "FINANCIAL_SECURITY",
        target: String? = "605000000",
        monthly: String? = "1470000",
        annualReturn: String? = "0.11",
        years: String? = "$TARGET_YEARS",
    ): String = mapOf(
        "name" to name,
        "level" to level?.let { "\"$it\"" },
        "targetAmount" to target,
        "monthlyInvestment" to monthly,
        "expectedAnnualReturn" to annualReturn,
        "targetYears" to years,
    ).filterValues { it != null }.entries.joinToString(",", "{", "}") { "\"${it.key}\":${it.value}" }

    private companion object {
        const val GOALS = "/api/v1/financial-goals"
        const val TARGET_YEARS = 16
        const val NAME_TOO_LONG = 151
        const val EXPECTED_GOALS_AFTER_DEFAULTS = 4
    }
}
