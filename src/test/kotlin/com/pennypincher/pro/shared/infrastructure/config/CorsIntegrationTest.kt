package com.pennypincher.pro.shared.infrastructure.config

import com.pennypincher.pro.support.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options

/** The test profile sets no `cors.allowed-origins`, so these tests also prove the local defaults. */
class CorsIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun should_answer_preflight_with_cors_headers_when_any_api_endpoint_is_requested() {
        PREFLIGHTS.forEach { (path, method) ->
            mockMvc.options(path) {
                header(HttpHeaders.ORIGIN, FLUTTER_ORIGIN)
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type, X-Request-ID")
            }.andExpect {
                status { isOk() }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_ORIGIN) }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true") }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS") }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type, X-Request-ID") }
                header { string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600") }
            }
        }
    }

    @Test
    fun should_accept_swagger_origin_when_default_local_origins_apply() {
        mockMvc.options("/api/v1/users/me") {
            header(HttpHeaders.ORIGIN, SWAGGER_ORIGIN)
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SWAGGER_ORIGIN) }
        }
    }

    @Test
    fun should_reject_preflight_when_origin_is_not_configured() {
        mockMvc.options("/api/v1/assets") {
            header(HttpHeaders.ORIGIN, "https://evil.example.com")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun should_reject_preflight_when_requested_header_is_not_allowed() {
        mockMvc.options("/api/v1/assets") {
            header(HttpHeaders.ORIGIN, FLUTTER_ORIGIN)
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Not-Allowed")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun should_reject_preflight_when_method_is_not_allowed() {
        mockMvc.options("/api/v1/users/me") {
            header(HttpHeaders.ORIGIN, FLUTTER_ORIGIN)
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun should_add_cors_headers_to_error_responses_so_the_browser_can_read_them() {
        mockMvc.get("/api/v1/users/me") { header(HttpHeaders.ORIGIN, FLUTTER_ORIGIN) }.andExpect {
            status { isUnauthorized() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FLUTTER_ORIGIN) }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true") }
            jsonPath("$.error.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun should_not_apply_cors_outside_the_api_when_origin_is_sent() {
        mockMvc.get("/actuator/health") { header(HttpHeaders.ORIGIN, FLUTTER_ORIGIN) }.andExpect {
            status { isOk() }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) }
        }
    }

    private companion object {
        const val FLUTTER_ORIGIN = "http://localhost:3000"
        const val SWAGGER_ORIGIN = "http://localhost:8080"
        val PREFLIGHTS =
            listOf(
                "/api/v1/auth/google/callback" to "POST",
                "/api/v1/auth/refresh" to "POST",
                "/api/v1/auth/logout" to "DELETE",
                "/api/v1/users/me" to "GET",
                "/api/v1/users/me" to "PATCH",
                "/api/v1/assets/123" to "PUT",
            )
    }
}
