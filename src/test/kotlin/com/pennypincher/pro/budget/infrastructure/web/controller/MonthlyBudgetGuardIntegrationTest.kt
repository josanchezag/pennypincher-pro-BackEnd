package com.pennypincher.pro.budget.infrastructure.web.controller

import com.jayway.jsonpath.JsonPath
import com.pennypincher.pro.budget.infrastructure.persistence.adapter.BudgetSourceRows
import com.pennypincher.pro.budget.infrastructure.persistence.repository.MonthlyBudgetJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/** Authentication, input validation, concurrency and overflow handling of the monthly budget endpoints. */
class MonthlyBudgetGuardIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var budgets: MonthlyBudgetJpaRepository

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun should_return_unauthorized_when_token_is_missing() {
        val id = UUID.randomUUID()
        listOf(
            mockMvc.get(BUDGETS),
            mockMvc.get("$BUDGETS/2026/10"),
            mockMvc.post(BUDGETS) { json("""{"year":2026,"month":10}""") },
            mockMvc.put("$BUDGETS/$id") { json("{}") },
            mockMvc.post("$BUDGETS/$id/recalculate"),
        ).forEach { result ->
            result.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error.code") { value("UNAUTHORIZED") }
            }
        }
    }

    @Test
    fun should_return_validation_error_with_field_when_create_body_breaks_a_rule() {
        val user = createUser()
        mapOf(
            """{"month":10}""" to "year",
            """{"year":2026}""" to "month",
            """{"year":1999,"month":10}""" to "year",
            """{"year":2101,"month":10}""" to "year",
            """{"year":2026,"month":0}""" to "month",
            """{"year":2026,"month":13}""" to "month",
            """{"year":2026,"month":10,"exchangeRate":0}""" to "exchangeRate",
            """{"year":2026,"month":10,"exchangeRate":-1}""" to "exchangeRate",
            """{"year":2026,"month":10,"exchangeRate":3300.001}""" to "exchangeRate",
            """{"year":2026,"month":10,"workDays":-1}""" to "workDays",
            """{"year":2026,"month":10,"workDays":32}""" to "workDays",
            """{"year":"abc","month":10}""" to "year",
            """{"year":2026.5,"month":11}""" to "year",
            """{"year":2026,"month":11,"workDays":21.7}""" to "workDays",
        ).forEach { (body, field) ->
            postJson(user, BUDGETS, body).andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                jsonPath("$.error.details[0].field") { value(field) }
            }
        }
        assertEquals(0, budgets.findAll().count { it.userId == user.id })
    }

    @Test
    fun should_return_validation_error_when_path_or_query_parameter_is_invalid() {
        val user = createUser()
        mapOf(
            "$BUDGETS/2026/13" to "month",
            "$BUDGETS/2026/0" to "month",
            "$BUDGETS/1999/1" to "year",
            "$BUDGETS/abc/1" to "year",
            "$BUDGETS?year=abc" to "year",
            "$BUDGETS?year=1999" to "year",
        ).forEach { (path, field) ->
            mockMvc.get(path) { bearer(user) }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                jsonPath("$.error.details[0].field") { value(field) }
            }
        }
        putJson(user, "$BUDGETS/not-a-uuid", "{}").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.details[0].field") { value("id") }
        }
        mockMvc.post("$BUDGETS/not-a-uuid/recalculate") { bearer(user) }.andExpect { status { isBadRequest() } }
        postJson(user, BUDGETS, "").andExpect { status { isBadRequest() } }
    }

    @Test
    fun should_store_one_budget_and_answer_conflict_to_the_rest_when_the_same_month_is_created_concurrently() {
        val user = createUser()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(PARALLEL_REQUESTS)
        val statuses = try {
            val calls = (1..PARALLEL_REQUESTS).map {
                Callable {
                    start.await()
                    postJson(user, BUDGETS, """{"year":2026,"month":10,"workDays":$it}""").andReturn().response.status
                }
            }
            val futures = calls.map(executor::submit)
            start.countDown()
            futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(1, statuses.count { it == HttpStatus.CREATED.value() }) { "Statuses: $statuses" }
        val conflicts = statuses.count { it == HttpStatus.CONFLICT.value() }
        assertEquals(PARALLEL_REQUESTS - 1, conflicts) { "Statuses: $statuses" }
        assertEquals(1, budgets.findAll().count { it.userId == user.id })
    }

    @Test
    fun should_return_validation_error_instead_of_conflict_when_a_total_overflows() {
        val user = createUser()
        val rows = BudgetSourceRows(jdbc)
        repeat(2) { rows.savings(user.id, MAX_AMOUNT) }

        postJson(user, BUDGETS, """{"year":2026,"month":10}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_FAILED") }
            jsonPath("$.error.details[0].field") { value("components.SAVINGS") }
        }
        assertEquals(0, budgets.findAll().count { it.userId == user.id })
    }

    @Test
    fun should_cap_percentage_when_amount_is_far_above_income() {
        val user = createUser()
        postJson(user, "/api/v1/income-sources", """{"name":"Poco","category":"OTHER","amount":0.01}""")
        BudgetSourceRows(jdbc).loan(user.id, MAX_AMOUNT)

        postJson(user, BUDGETS, """{"year":2026,"month":10}""").andExpect {
            status { isCreated() }
            content { string(containsString("\"loanPercentage\":99999.99")) }
        }
    }

    @Test
    fun should_return_not_found_for_budgets_of_other_users_or_unknown_ids_and_months() {
        val owner = createUser()
        val id = idOf(postJson(owner, BUDGETS, """{"year":2026,"month":10,"workDays":22}"""))
        val intruder = createUser()

        listOf(
            mockMvc.get("$BUDGETS/2026/10") { bearer(intruder) },
            mockMvc.get("$BUDGETS/2026/11") { bearer(owner) },
            putJson(intruder, "$BUDGETS/$id", """{"workDays":1}"""),
            putJson(owner, "$BUDGETS/${UUID.randomUUID()}", """{"workDays":1}"""),
            mockMvc.post("$BUDGETS/$id/recalculate") { bearer(intruder) },
        ).forEach { result ->
            result.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("NOT_FOUND") }
            }
        }
        mockMvc.get(BUDGETS) { bearer(intruder) }.andExpect { jsonPath("$.data") { isEmpty() } }
        assertEquals(22, budgets.findById(id).orElseThrow().workDays)
    }

    @Test
    fun should_answer_put_and_recalculate_with_the_stored_budget_so_a_later_get_is_identical() {
        val user = createUser()
        val created = postJson(user, BUDGETS, """{"year":2026,"month":10}""")
        val id = idOf(created)
        assertEquals(dataOf(created), dataOf(mockMvc.get("$BUDGETS/2026/10") { bearer(user) }))

        val updated = dataOf(putJson(user, "$BUDGETS/$id", """{"workDays":22}"""))
        assertEquals(updated, dataOf(mockMvc.get("$BUDGETS/2026/10") { bearer(user) }))
        val recalculated = dataOf(mockMvc.post("$BUDGETS/$id/recalculate") { bearer(user) })
        assertEquals(recalculated, dataOf(mockMvc.get("$BUDGETS/2026/10") { bearer(user) }))
    }

    private fun dataOf(result: ResultActionsDsl): Map<String, Any?> =
        JsonPath.read(result.andReturn().response.contentAsString, "$.data") ?: emptyMap()

    private companion object {
        const val BUDGETS = "/api/v1/monthly-budgets"
        const val PARALLEL_REQUESTS = 8
        const val TIMEOUT_SECONDS = 60L
        const val MAX_AMOUNT = "9999999999999999.99"
    }
}
