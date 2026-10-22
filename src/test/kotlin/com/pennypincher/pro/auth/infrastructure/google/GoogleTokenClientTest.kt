package com.pennypincher.pro.auth.infrastructure.google

import com.pennypincher.pro.auth.domain.exception.GoogleAuthenticationException
import com.pennypincher.pro.auth.domain.exception.IdentityProviderUnavailableException
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode
import com.pennypincher.pro.support.FakeGoogleServer
import com.pennypincher.pro.support.GoogleIdTokens
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.web.client.RestClient

class GoogleTokenClientTest {
    private val tokens = GoogleIdTokens()
    private val server = FakeGoogleServer(tokens)
    private val circuitBreaker =
        CircuitBreaker.of(
            "google-token-test",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .ignoreExceptions(GoogleAuthenticationException::class.java)
                .build(),
        )
    private val client = GoogleTokenClient(endpointClient(), verifier(), circuitBreaker)
    private val authorization = GoogleAuthorizationCode("auth-code", "http://localhost:3000/auth.html", "verifier-123")

    @AfterEach
    fun stopServer() = server.close()

    @Test
    fun should_return_verified_identity_when_google_accepts_the_code() {
        server.respondWithIdToken(tokens.sign())

        val identity = client.authenticate(authorization)

        assertEquals("google-sub-123", identity.subject)
        assertEquals("ana@example.com", identity.profile.email)
    }

    @Test
    fun should_send_code_verifier_secret_and_redirect_uri_when_exchanging_the_code() {
        server.respondWithIdToken(tokens.sign())

        client.authenticate(authorization)

        val expected =
            mapOf(
                "grant_type" to "authorization_code",
                "code" to "auth-code",
                "code_verifier" to "verifier-123",
                "redirect_uri" to "http://localhost:3000/auth.html",
                "client_id" to GoogleIdTokens.CLIENT_ID,
                "client_secret" to CLIENT_SECRET,
            )
        assertEquals(expected, server.lastTokenForm)
    }

    @Test
    fun should_fail_authentication_when_google_rejects_the_code() {
        server.respondWithError(BAD_REQUEST)

        assertThrows<GoogleAuthenticationException> { client.authenticate(authorization) }
    }

    @Test
    fun should_fail_authentication_when_google_returns_no_id_token() {
        server.tokenBody = """{"access_token":"ya29.fake"}"""

        assertThrows<GoogleAuthenticationException> { client.authenticate(authorization) }
    }

    @Test
    fun should_fail_authentication_when_returned_id_token_is_invalid() {
        server.respondWithIdToken(tokens.sign(GoogleIdTokens.Claims(audience = "other-client")))

        assertThrows<GoogleAuthenticationException> { client.authenticate(authorization) }
    }

    @Test
    fun should_fail_authentication_when_request_is_invalid_for_the_code_or_verifier() {
        server.respondWithError(BAD_REQUEST, "invalid_request")

        assertThrows<GoogleAuthenticationException> { client.authenticate(authorization) }
    }

    @Test
    fun should_report_unavailable_when_google_fails() {
        server.respondWithError(SERVER_ERROR)

        assertThrows<IdentityProviderUnavailableException> { client.authenticate(authorization) }
    }

    @Test
    fun should_report_unavailable_when_client_credentials_rate_limit_or_other_errors_occur() {
        listOf(
            UNAUTHORIZED to "invalid_client",
            TOO_MANY_REQUESTS to "rate_limit_exceeded",
            BAD_REQUEST to "unsupported_grant_type",
        )
            .forEach { (status, error) ->
                server.respondWithError(status, error)
                assertThrows<IdentityProviderUnavailableException> { client.authenticate(authorization) }
            }
    }

    @Test
    fun should_open_circuit_when_google_signing_keys_cannot_be_fetched() {
        server.respondWithIdToken(tokens.sign())
        server.certsStatus = SERVER_ERROR

        repeat(2) { assertThrows<IdentityProviderUnavailableException> { client.authenticate(authorization) } }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
    }

    @Test
    fun should_fail_fast_without_calling_google_when_circuit_is_open() {
        server.respondWithError(SERVER_ERROR)
        repeat(2) { assertThrows<IdentityProviderUnavailableException> { client.authenticate(authorization) } }
        val callsBeforeOpen = server.tokenCalls

        assertThrows<IdentityProviderUnavailableException> { client.authenticate(authorization) }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
        assertEquals(callsBeforeOpen, server.tokenCalls)
    }

    @Test
    fun should_keep_circuit_closed_when_codes_are_rejected() {
        server.respondWithError(BAD_REQUEST)

        repeat(3) { assertThrows<GoogleAuthenticationException> { client.authenticate(authorization) } }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.state)
    }

    private fun endpointClient() =
        GoogleTokenEndpointClient(RestClient.create(), server.tokenUri, GoogleIdTokens.CLIENT_ID, CLIENT_SECRET)

    private fun verifier() = GoogleIdTokenVerifier(
        NimbusJwtDecoder.withJwkSetUri(server.jwkSetUri).build()
            .apply { setJwtValidator(GoogleIdTokenValidators.create(GoogleIdTokens.CLIENT_ID)) },
    )

    private companion object {
        const val CLIENT_SECRET = "test-secret"
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR = 500
    }
}
