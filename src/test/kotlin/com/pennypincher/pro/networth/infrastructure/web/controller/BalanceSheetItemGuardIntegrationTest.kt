package com.pennypincher.pro.networth.infrastructure.web.controller

import com.pennypincher.pro.support.ApiIntegrationTest
import java.util.UUID
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/** Authentication and input validation of the asset and liability endpoints, which share one implementation. */
class BalanceSheetItemGuardIntegrationTest : ApiIntegrationTest() {
    @ParameterizedTest
    @MethodSource("endpoints")
    fun should_return_unauthorized_when_token_is_missing(endpoint: Endpoint) {
        val id = UUID.randomUUID()
        val body = endpoint.body("Caja", "1", endpoint.category)
        listOf(
            mockMvc.get(endpoint.path),
            mockMvc.get("${endpoint.path}/total"),
            mockMvc.get("${endpoint.path}/$id"),
            mockMvc.post(endpoint.path) { json(body) },
            mockMvc.put("${endpoint.path}/$id") { json(body) },
            mockMvc.delete("${endpoint.path}/$id"),
        ).forEach { result ->
            result.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error.code") { value("UNAUTHORIZED") }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    fun should_return_validation_error_when_category_filter_is_unknown(endpoint: Endpoint) {
        listOf("YACHT", endpoint.category.lowercase()).forEach { category ->
            mockMvc.get("${endpoint.path}?category=$category") { bearer(createUser()) }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                jsonPath("$.error.details[0].field") { value("category") }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    fun should_return_validation_error_with_field_when_body_breaks_a_rule(endpoint: Endpoint) {
        val user = createUser()
        val amount = endpoint.amountField
        val category = endpoint.category
        mapOf(
            """{"$amount":1,"category":"$category"}""" to "name",
            """{"name":"Caja","category":"$category"}""" to amount,
            """{"name":"Caja","$amount":1}""" to "category",
            endpoint.body("   ", "1", category) to "name",
            endpoint.body("x".repeat(NAME_TOO_LONG), "1", category) to "name",
            endpoint.body("Caja", "-0.01", category) to amount,
            endpoint.body("Caja", "1.001", category) to amount,
            endpoint.body("Caja", "12345678901234567", category) to amount,
            endpoint.body("Caja", "\"abc\"", category) to amount,
            endpoint.body("Caja", "1", "YACHT") to "category",
        ).forEach { (body, field) ->
            postJson(user, endpoint.path, body).andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                jsonPath("$.error.details[0].field") { value(field) }
            }
        }
        mockMvc.get(endpoint.path) { bearer(user) }.andExpect { jsonPath("$.data") { isEmpty() } }
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    fun should_return_validation_error_when_body_is_missing_or_id_is_not_a_uuid(endpoint: Endpoint) {
        val user = createUser()
        postJson(user, endpoint.path, "").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_FAILED") }
        }
        deleteAs(user, "${endpoint.path}/not-a-uuid").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.details[0].field") { value("id") }
        }
    }

    /** One CRUD resource: its path, the name of its amount field and one of its categories. */
    data class Endpoint(val path: String, val amountField: String, val category: String) {
        fun body(name: String, amount: String, category: String): String =
            """{"name":"$name","$amountField":$amount,"category":"$category"}"""
    }

    companion object {
        private const val NAME_TOO_LONG = 151

        @JvmStatic
        fun endpoints(): List<Endpoint> = listOf(
            Endpoint("/api/v1/assets", "value", "CASH"),
            Endpoint("/api/v1/liabilities", "currentBalance", "CREDIT_CARD"),
        )
    }
}
