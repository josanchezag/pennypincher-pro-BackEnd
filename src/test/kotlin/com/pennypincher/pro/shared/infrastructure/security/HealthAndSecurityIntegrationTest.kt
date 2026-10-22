package com.pennypincher.pro.shared.infrastructure.security

import com.pennypincher.pro.support.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options

class HealthAndSecurityIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun should_report_up_when_health_is_requested_without_token() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
        }
    }

    @Test
    fun should_report_up_when_kubernetes_probes_are_requested() {
        listOf("/actuator/health/liveness", "/actuator/health/readiness").forEach { probe ->
            mockMvc.get(probe).andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UP") }
            }
        }
    }

    @Test
    fun should_return_unauthorized_when_api_is_called_without_token() {
        mockMvc.get("/api/v1/users/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun should_hide_other_actuator_endpoints_when_called_without_token() {
        mockMvc.get("/actuator/env").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun should_hide_api_docs_when_swagger_is_disabled() {
        mockMvc.get("/v3/api-docs").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun should_allow_preflight_when_origin_is_configured() {
        mockMvc
            .options("/api/v1/assets") {
                header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type, X-Request-ID")
            }.andExpect {
                status { isOk() }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN) }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true") }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS") }
                header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type, X-Request-ID") }
                header { string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600") }
            }
    }

    @Test
    fun should_reject_preflight_when_requested_header_is_not_allowed() {
        mockMvc
            .options("/api/v1/assets") {
                header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Not-Allowed")
            }.andExpect { status { isForbidden() } }
    }

    @Test
    fun should_reject_preflight_when_origin_is_not_configured() {
        mockMvc
            .options("/api/v1/assets") {
                header(HttpHeaders.ORIGIN, "https://evil.example.com")
                header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            }.andExpect { status { isForbidden() } }
    }

    private companion object {
        const val ALLOWED_ORIGIN = "http://localhost:3000"
    }
}
