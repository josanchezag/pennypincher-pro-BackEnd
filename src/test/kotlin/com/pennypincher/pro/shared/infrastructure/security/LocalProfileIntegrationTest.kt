package com.pennypincher.pro.shared.infrastructure.security

import com.pennypincher.pro.support.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/** Runs with the `test` and `local` profiles, so the local-only settings of application.yml apply. */
@ActiveProfiles("local")
class LocalProfileIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun should_serve_api_docs_without_token_when_local_profile_is_active() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.openapi") { exists() }
        }
    }

    @Test
    fun should_serve_swagger_ui_without_token_when_local_profile_is_active() {
        mockMvc.get("/swagger-ui/index.html").andExpect { status { isOk() } }
    }
}
