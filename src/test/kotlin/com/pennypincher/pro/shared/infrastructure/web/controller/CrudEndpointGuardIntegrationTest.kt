package com.pennypincher.pro.shared.infrastructure.web.controller

import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.util.UUID
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/**
 * Authentication, ownership and input validation of the per-user CRUD endpoints, which share one
 * implementation.
 */
class CrudEndpointGuardIntegrationTest : ApiIntegrationTest() {
    @ParameterizedTest
    @MethodSource("endpoints")
    fun should_return_unauthorized_when_token_is_missing(endpoint: Endpoint) {
        val id = UUID.randomUUID()
        val body = endpoint.body()
        listOf(
            mockMvc.get(endpoint.path),
            mockMvc.get("${endpoint.path}/${endpoint.aggregate}"),
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
    fun should_return_not_found_when_record_belongs_to_another_user_or_was_deleted(endpoint: Endpoint) {
        val owner = createUser()
        val id = idOf(postJson(owner, endpoint.path, endpoint.body()))
        val recordPath = "${endpoint.path}/$id"

        expectNotFound(createUser(), recordPath, endpoint.body())
        mockMvc.get(recordPath) { bearer(owner) }.andExpect { status { isOk() } }
        deleteAs(owner, recordPath).andExpect { status { isNoContent() } }
        expectNotFound(owner, recordPath, endpoint.body())
        mockMvc.get(endpoint.path) { bearer(owner) }.andExpect { jsonPath("$.data") { isEmpty() } }
    }

    @ParameterizedTest
    @MethodSource("categorizedEndpoints")
    fun should_return_validation_error_when_category_filter_is_unknown(endpoint: Endpoint) {
        listOf("YACHT", checkNotNull(endpoint.category).lowercase()).forEach { category ->
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
        invalidBodies(endpoint).forEach { (body, field) ->
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

    @ParameterizedTest
    @MethodSource("endpoints")
    fun should_treat_the_aggregate_path_not_opted_into_as_an_id_when_requesting_it(endpoint: Endpoint) {
        val other = AGGREGATES.single { it != endpoint.aggregate }

        mockMvc.get("${endpoint.path}/$other") { bearer(createUser()) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.details[0].field") { value("id") }
        }
    }

    private fun expectNotFound(caller: User, path: String, body: String) {
        listOf(mockMvc.get(path) { bearer(caller) }, putJson(caller, path, body), deleteAs(caller, path)).forEach {
            it.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("NOT_FOUND") }
            }
        }
    }

    private fun invalidBodies(endpoint: Endpoint): List<Pair<String, String>> = buildList {
        add(endpoint.body(NAME to null) to NAME)
        add(endpoint.body(NAME to "\"   \"") to NAME)
        add(endpoint.body(NAME to "\"${"x".repeat(NAME_TOO_LONG)}\"") to NAME)
        endpoint.amountFields.forEach { field ->
            add(endpoint.body(field to null) to field)
            INVALID_AMOUNTS.forEach { amount -> add(endpoint.body(field to amount) to field) }
        }
        if (endpoint.category != null) {
            add(endpoint.body(CATEGORY to null) to CATEGORY)
            add(endpoint.body(CATEGORY to "\"YACHT\"") to CATEGORY)
        }
    }

    /**
     * One CRUD resource: its path, its opted-in aggregate path, its required amount fields and one of its
     * categories (null when the records have none).
     */
    data class Endpoint(
        val path: String,
        val aggregate: String,
        val amountFields: List<String>,
        val category: String? = null,
    ) {
        /** A valid JSON body; each override replaces a field's raw JSON value, or drops the field when null. */
        fun body(vararg overrides: Pair<String, String?>): String {
            val fields = linkedMapOf<String, String?>(NAME to "\"Caja\"")
            amountFields.forEach { fields[it] = "1" }
            category?.let { fields[CATEGORY] = "\"$it\"" }
            fields.putAll(overrides)
            return fields.entries.filter {
                it.value != null
            }.joinToString(",", "{", "}") { "\"${it.key}\":${it.value}" }
        }

        override fun toString(): String = path
    }

    companion object {
        private const val NAME = "name"
        private const val CATEGORY = "category"
        private const val NAME_TOO_LONG = 151
        private const val TOTAL = "total"
        private const val SUMMARY = "summary"
        private val AGGREGATES = listOf(TOTAL, SUMMARY)
        private val INVALID_AMOUNTS = listOf("-0.01", "1.001", "12345678901234567", "\"abc\"")

        @JvmStatic
        fun endpoints(): List<Endpoint> = listOf(
            Endpoint("/api/v1/assets", TOTAL, listOf("value"), "CASH"),
            Endpoint("/api/v1/liabilities", TOTAL, listOf("currentBalance"), "CREDIT_CARD"),
            Endpoint("/api/v1/income-sources", TOTAL, listOf("amount"), "EMPLOYMENT"),
            Endpoint("/api/v1/credit-cards", SUMMARY, listOf("monthlyPayment", "totalBalance")),
            Endpoint("/api/v1/loans", SUMMARY, listOf("monthlyPayment", "totalBalance")),
        )

        @JvmStatic
        fun categorizedEndpoints(): List<Endpoint> = endpoints().filter { it.category != null }
    }
}
