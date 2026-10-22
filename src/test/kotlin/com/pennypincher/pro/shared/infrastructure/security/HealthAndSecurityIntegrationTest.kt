package com.pennypincher.pro.shared.infrastructure.security

import com.pennypincher.pro.support.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

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
        mockMvc.get("/api/v1/users/me").andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("UNAUTHORIZED") }
            jsonPath("$.error.message") { isNotEmpty() }
        }
    }

    @Test
    fun should_hide_other_actuator_endpoints_when_called_without_token() {
        mockMvc.get("/actuator/env").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun should_hide_api_docs_when_swagger_is_disabled() {
        mockMvc.get("/v3/api-docs").andExpect { status { isUnauthorized() } }
    }
}
