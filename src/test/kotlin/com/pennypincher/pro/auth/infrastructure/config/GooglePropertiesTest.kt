package com.pennypincher.pro.auth.infrastructure.config

import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class GooglePropertiesTest {
    @Test
    fun should_accept_empty_credentials_when_they_are_not_required() {
        assertDoesNotThrow { properties(clientId = "", clientSecret = "", required = false) }
    }

    @Test
    fun should_fail_when_required_credentials_are_blank_or_unresolved() {
        val blank = assertThrows<IllegalArgumentException> { properties(clientId = " ", required = true) }
        assertEquals("google.client-id is required (set its environment variable)", blank.message)
        assertThrows<IllegalArgumentException> {
            properties(clientSecret = "\${GOOGLE_CLIENT_SECRET}", required = true)
        }
    }

    @Test
    fun should_fail_when_credentials_are_unresolved_placeholders_even_if_optional() {
        assertThrows<IllegalArgumentException> { properties(clientId = "\${GOOGLE_CLIENT_ID}", required = false) }
    }

    @Test
    fun should_fail_when_redirect_uris_are_missing_blank_or_unresolved() {
        listOf(emptyList(), listOf(" "), listOf("\${GOOGLE_ALLOWED_REDIRECT_URIS}")).forEach { uris ->
            assertThrows<IllegalArgumentException> { properties(redirectUris = uris) }
        }
    }

    private fun properties(
        clientId: String = "client-id",
        clientSecret: String = "secret",
        redirectUris: List<String> = listOf("http://localhost:3000/auth.html"),
        required: Boolean = true,
    ) = GoogleProperties(
        clientId = clientId,
        clientSecret = clientSecret,
        tokenUri = "https://oauth2.googleapis.com/token",
        jwkSetUri = "https://www.googleapis.com/oauth2/v3/certs",
        allowedRedirectUris = redirectUris,
        connectTimeout = Duration.ofSeconds(5),
        readTimeout = Duration.ofSeconds(10),
        credentialsRequired = required,
    )
}
