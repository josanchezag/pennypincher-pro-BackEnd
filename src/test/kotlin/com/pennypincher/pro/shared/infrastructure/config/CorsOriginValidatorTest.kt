package com.pennypincher.pro.shared.infrastructure.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CorsOriginValidatorTest {
    @Test
    fun should_return_trimmed_origins_when_all_are_valid() {
        val origins =
            CorsOriginValidator.validate(
                listOf(" http://localhost:3000 ", "https://pennypincher-pro.vercel.app", ""),
            )

        assertEquals(listOf("http://localhost:3000", "https://pennypincher-pro.vercel.app"), origins)
    }

    @Test
    fun should_reject_origins_when_list_is_blank() {
        assertThrows<IllegalArgumentException> { CorsOriginValidator.validate(listOf(" ")) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "\${CORS_ALLOWED_ORIGINS}",
            "*",
            "https://*.example.com",
            "ftp://example.com",
            "localhost:3000",
            "https://example.com/app",
            "https://example.com/",
            "https://example.com?x=1",
            "https://user@example.com",
            "https://exa mple.com",
        ],
    )
    fun should_reject_origin_when_it_is_not_an_absolute_http_origin(origin: String) {
        assertThrows<IllegalArgumentException> { CorsOriginValidator.validate(listOf(origin)) }
    }
}
