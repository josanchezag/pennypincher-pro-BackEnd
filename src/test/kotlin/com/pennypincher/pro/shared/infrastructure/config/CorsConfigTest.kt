package com.pennypincher.pro.shared.infrastructure.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CorsConfigTest {
    private val config = CorsConfig()

    @Test
    fun should_allow_credentials_when_origins_are_valid() {
        val cors = config.buildCorsConfiguration(listOf(" http://localhost:3000 ", "https://app.example.com"))

        assertEquals(listOf("http://localhost:3000", "https://app.example.com"), cors.allowedOrigins)
        assertTrue(cors.allowCredentials == true)
        assertEquals(3600L, cors.maxAge)
    }

    @Test
    fun should_reject_configuration_when_origin_is_unresolved_placeholder() {
        assertThrows<IllegalArgumentException> { config.buildCorsConfiguration(listOf("\${CORS_ALLOWED_ORIGINS}")) }
    }
}
