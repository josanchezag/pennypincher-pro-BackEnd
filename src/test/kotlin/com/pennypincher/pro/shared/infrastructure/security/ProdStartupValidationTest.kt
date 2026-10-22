package com.pennypincher.pro.shared.infrastructure.security

import com.pennypincher.pro.PennyPincherProApplication
import com.pennypincher.pro.support.TestDatabases
import com.pennypincher.pro.support.TestRsaKeys
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.builder.SpringApplicationBuilder

/** The `prod` profile must refuse to start when a mandatory secret is missing, instead of starting UP. */
class ProdStartupValidationTest {
    @Test
    fun should_fail_to_start_when_google_credentials_are_missing_in_prod() {
        val (privatePem, publicPem) = TestRsaKeys.base64Pem(TestRsaKeys.applicationKeys)

        val message = startupFailure("jwt.private-key=$privatePem", "jwt.public-key=$publicPem")

        assertTrue("google.client-id is required" in message, message)
    }

    @Test
    fun should_fail_to_start_when_jwt_keys_are_missing_in_prod() {
        val message =
            startupFailure(
                "google.client-id=client-id",
                "google.client-secret=secret",
                "google.allowed-redirect-uris=https://pennypincher-pro.vercel.app/auth.html",
            )

        assertTrue("jwt.private-key and jwt.public-key are required" in message, message)
    }

    private fun startupFailure(vararg extraProperties: String): String {
        val failure = assertThrows<Exception> {
            SpringApplicationBuilder(PennyPincherProApplication::class.java)
                .profiles("prod")
                .run(*(baseProperties() + extraProperties).map { "--$it" }.toTypedArray())
                .close()
        }
        return generateSequence<Throwable>(failure) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
    }

    private fun baseProperties() = arrayOf(
        "server.port=0",
        "spring.datasource.url=${TestDatabases.current.jdbcUrl}",
        "spring.datasource.username=${TestDatabases.current.username}",
        "spring.datasource.password=${TestDatabases.current.password}",
        "cors.allowed-origins=https://pennypincher-pro.vercel.app",
    )
}
