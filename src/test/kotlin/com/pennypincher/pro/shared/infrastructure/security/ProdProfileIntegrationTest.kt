package com.pennypincher.pro.shared.infrastructure.security

import com.pennypincher.pro.support.IntegrationTest
import com.pennypincher.pro.support.TestRsaKeys
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/** Smoke test of the `prod` profile: only the health probes and the auth endpoints are reachable without a token. */
@ActiveProfiles(profiles = ["prod"], inheritProfiles = false)
@TestPropertySource(
    properties = [
        "cors.allowed-origins=https://pennypincher-pro.vercel.app",
        "google.client-id=test-client-id",
        "google.client-secret=test-client-secret",
        "google.allowed-redirect-uris=https://pennypincher-pro.vercel.app/auth.html",
    ],
)
class ProdProfileIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun should_report_up_when_health_is_requested_in_prod() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
        }
    }

    @Test
    fun should_hide_api_docs_and_swagger_when_prod_profile_is_active() {
        listOf("/v3/api-docs", "/swagger-ui.html", "/swagger-ui/index.html").forEach { path ->
            mockMvc.get(path).andExpect { status { isUnauthorized() } }
        }
    }

    @Test
    fun should_hide_non_health_actuator_endpoints_when_prod_profile_is_active() {
        listOf("/actuator", "/actuator/env", "/actuator/beans").forEach { path ->
            mockMvc.get(path).andExpect { status { isUnauthorized() } }
        }
    }

    @Test
    fun should_not_expose_dev_login_when_prod_profile_is_active() {
        mockMvc.post("/api/v1/auth/dev-login").andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("NOT_FOUND") }
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerKeys(registry: DynamicPropertyRegistry) {
            val (privatePem, publicPem) = TestRsaKeys.base64Pem(TestRsaKeys.applicationKeys)
            registry.add("jwt.private-key") { privatePem }
            registry.add("jwt.public-key") { publicPem }
        }
    }
}
