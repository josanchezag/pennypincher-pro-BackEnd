package com.pennypincher.pro.goals.infrastructure.web.controller

import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class GoalProjectionIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var clock: Clock

    @Test
    fun should_project_goal_from_live_net_worth_of_active_assets_and_liabilities() {
        val user = createUser()
        postJson(user, "/api/v1/assets", """{"name":"Casa","value":500000000,"category":"REAL_ESTATE"}""")
        postJson(
            user,
            "/api/v1/liabilities",
            """{"name":"Hipoteca","currentBalance":76717227,"category":"BANK_LOAN"}""",
        )
        val deleted = idOf(postJson(user, "/api/v1/assets", """{"name":"Vieja","value":9,"category":"CASH"}"""))
        deleteAs(user, "/api/v1/assets/$deleted")
        val id = createGoal(user, SECURITY_GOAL)
        val month = currentMonth()

        mockMvc.get("$GOALS/$id/projection") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.goalId") { value(id.toString()) }
            jsonPath("$.data.name") { value("Seguridad Financiera") }
            jsonPath("$.data.level") { value("FINANCIAL_SECURITY") }
            jsonPath("$.data.reachable") { value(true) }
            jsonPath("$.data.targetYears") { value(TARGET_YEARS) }
            jsonPath("$.data.projectedMonthsToGoal") { value(EXCEL_SECURITY_MONTHS) }
            jsonPath("$.data.projectedAchievementDate") {
                value(month.plusMonths(EXCEL_SECURITY_MONTHS.toLong()).toString())
            }
            jsonPath("$.data.projectionDataPoints") { value(hasSize<Any>(EXCEL_SECURITY_MONTHS + 1)) }
            jsonPath("$.data.projectionDataPoints[0].month") { value(0) }
            jsonPath("$.data.projectionDataPoints[0].date") { value(month.toString()) }
            content {
                string(containsString("\"currentNetWorth\":423282773.00,\"progressAmount\":423282773.00"))
                string(containsString("\"progressPercentage\":69.96,\"remainingAmount\":181717227.00"))
                string(containsString("\"expectedAnnualReturn\":0.1100"))
                string(containsString("\"projectedYearsToGoal\":2.58"))
                string(containsString("\"projectedValue\":423282773.00"))
            }
        }
    }

    @Test
    fun should_update_projection_when_net_worth_or_monthly_investment_changes() {
        val user = createUser()
        val id = createGoal(user, SECURITY_GOAL)
        mockMvc.get("$GOALS/$id/projection") { bearer(user) }.andExpect {
            jsonPath("$.data.currentNetWorth") { value(0.0) }
            jsonPath("$.data.projectedMonthsToGoal") { value(SECURITY_MONTHS_FROM_ZERO) }
        }

        postJson(user, "/api/v1/assets", """{"name":"Todo","value":605000000,"category":"CASH"}""")
        mockMvc.get("$GOALS/$id/projection") { bearer(user) }.andExpect {
            jsonPath("$.data.progressPercentage") { value(FULL_PROGRESS) }
            jsonPath("$.data.projectedMonthsToGoal") { value(0) }
            jsonPath("$.data.projectionDataPoints") { value(hasSize<Any>(MIN_POINTS)) }
        }
    }

    @Test
    fun should_report_unreachable_goal_with_null_projection_fields() {
        val user = createUser()
        postJson(user, "/api/v1/liabilities", """{"name":"Deuda","currentBalance":1000,"category":"OTHER"}""")
        val id = createGoal(user, SECURITY_GOAL.replace("\"monthlyInvestment\":1470000", "\"monthlyInvestment\":0"))

        mockMvc.get("$GOALS/$id/projection") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.reachable") { value(false) }
            jsonPath("$.data.progressPercentage") { value(0.0) }
            jsonPath("$.data.projectionDataPoints") { value(hasSize<Any>(MAX_POINTS)) }
            content {
                string(containsString("\"currentNetWorth\":-1000.00"))
                string(containsString("\"projectedMonthsToGoal\":null"))
                string(containsString("\"projectedYearsToGoal\":null"))
                string(containsString("\"projectedAchievementDate\":null"))
            }
        }
    }

    @Test
    fun should_return_all_projections_in_goal_order_without_being_captured_by_the_id_route() {
        val user = createUser()
        mockMvc.get("$GOALS/all-projections") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isEmpty() }
        }
        mockMvc.post("$GOALS/defaults") { bearer(user) }
        createGoal(createUser(), SECURITY_GOAL)

        mockMvc.get("$GOALS/all-projections") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") {
                value(
                    contains(
                        "Seguridad Financiera",
                        "Independencia Financiera",
                        "Libertad Financiera",
                        "Libertad Absoluta",
                    ),
                )
            }
            jsonPath("$.data[*].projectedMonthsToGoal") { value(contains(176, 158, 147, 141)) }
        }
    }

    @Test
    fun should_return_not_found_for_projection_of_another_users_or_unknown_goal() {
        val id = createGoal(createUser(), SECURITY_GOAL)
        val intruder = createUser()

        listOf("$GOALS/$id/projection", "$GOALS/${UUID.randomUUID()}/projection").forEach { path ->
            mockMvc.get(path) { bearer(intruder) }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("NOT_FOUND") }
            }
        }
        mockMvc.get("$GOALS/not-a-uuid/projection") { bearer(intruder) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.details[0].field") { value("id") }
        }
        mockMvc.get("$GOALS/$id/projection").andExpect { status { isUnauthorized() } }
        mockMvc.get("$GOALS/all-projections").andExpect { status { isUnauthorized() } }
    }

    private fun currentMonth(): LocalDate = LocalDate.now(clock).withDayOfMonth(1)

    private fun createGoal(user: User, body: String): UUID = idOf(postJson(user, GOALS, body))

    private companion object {
        const val GOALS = "/api/v1/financial-goals"
        const val SECURITY_GOAL =
            """{"name":"Seguridad Financiera","level":"FINANCIAL_SECURITY","targetAmount":605000000,""" +
                """"monthlyInvestment":1470000,"expectedAnnualReturn":0.11,"targetYears":16}"""
        const val TARGET_YEARS = 16
        const val EXCEL_SECURITY_MONTHS = 31
        const val SECURITY_MONTHS_FROM_ZERO = 176
        const val FULL_PROGRESS = 100.0
        const val MIN_POINTS = 13
        const val MAX_POINTS = 601
    }
}
