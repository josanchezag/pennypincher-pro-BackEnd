package com.pennypincher.pro.auth.infrastructure.web.controller

import com.ninjasquad.springmockk.MockkBean
import com.pennypincher.pro.auth.application.usecase.RefreshTokenCodec
import com.pennypincher.pro.auth.domain.exception.GoogleAuthenticationException
import com.pennypincher.pro.auth.domain.exception.IdentityProviderUnavailableException
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode
import com.pennypincher.pro.auth.domain.model.VerifiedIdentity
import com.pennypincher.pro.auth.domain.port.outbound.IdentityProviderPort
import com.pennypincher.pro.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository
import com.pennypincher.pro.support.IntegrationTest
import com.pennypincher.pro.user.domain.model.IdentityProfile
import com.pennypincher.pro.user.infrastructure.persistence.repository.UserJpaRepository
import io.mockk.every
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

class GoogleCallbackIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var users: UserJpaRepository

    @Autowired
    private lateinit var refreshTokens: RefreshTokenJpaRepository

    @MockkBean
    private lateinit var identityProvider: IdentityProviderPort

    @Test
    fun should_create_user_and_issue_tokens_when_google_code_is_valid() {
        val identity = identityFor("first@example.com", "First Login")
        every { identityProvider.authenticate(authorization()) } returns identity

        val result = callback(validBody()).andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { isNotEmpty() }
            jsonPath("$.data.tokenType") { value("Bearer") }
            jsonPath("$.data.expiresIn") { value(900) }
            jsonPath("$.data.user.email") { value("first@example.com") }
            jsonPath("$.data.user.name") { value("First Login") }
            jsonPath("$.data.user.id") { isNotEmpty() }
            jsonPath("$.data.user.createdAt") { isNotEmpty() }
            jsonPath("$.meta.requestId") { isNotEmpty() }
            jsonPath("$.meta.timestamp") { isNotEmpty() }
            header { string(HttpHeaders.CACHE_CONTROL, "no-store") }
        }.andReturn()

        val cookie = result.response.getHeader(HttpHeaders.SET_COOKIE).orEmpty()
        assertCookieAttributes(cookie)
        val user = users.findByGoogleSub(identity.subject)
        assertNotNull(user)
        val stored = refreshTokens.findAll().single { it.userId == user!!.id }
        assertEquals(RefreshTokenCodec().hash(cookieValue(cookie)), stored.tokenHash)
    }

    @Test
    fun should_update_existing_user_when_profile_changed_in_google() {
        val subject = "google-${UUID.randomUUID()}"
        every { identityProvider.authenticate(authorization()) } returns identityFor("old@example.com", "Old", subject)
        callback(validBody()).andExpect { status { isOk() } }
        every { identityProvider.authenticate(authorization()) } returns identityFor("new@example.com", "New", subject)

        callback(validBody()).andExpect {
            status { isOk() }
            jsonPath("$.data.user.email") { value("new@example.com") }
        }

        val user = users.findByGoogleSub(subject)!!
        assertEquals("new@example.com", user.email)
        assertEquals("New", user.name)
    }

    @Test
    fun should_return_validation_details_when_fields_are_blank() {
        callback("""{"code":" ","redirectUri":"$REDIRECT_URI"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_FAILED") }
            jsonPath("$.error.details.length()") { value(2) }
            jsonPath("$.meta.requestId") { isNotEmpty() }
        }
    }

    @Test
    fun should_reject_request_when_body_is_malformed() {
        callback("{not json").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_FAILED") }
        }
    }

    @Test
    fun should_reject_request_when_redirect_uri_is_not_allowed() {
        callback(validBody(redirectUri = "https://evil.example.com/auth.html")).andExpect {
            status { isBadRequest() }
            jsonPath("$.error.details[0].field") { value("redirectUri") }
        }
    }

    @Test
    fun should_return_unauthorized_envelope_when_google_rejects_the_code() {
        every { identityProvider.authenticate(any()) } throws GoogleAuthenticationException("rejected")

        callback(validBody()).andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("GOOGLE_AUTH_FAILED") }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }
    }

    @Test
    fun should_return_service_unavailable_when_google_is_down() {
        every { identityProvider.authenticate(any()) } throws IdentityProviderUnavailableException("down")

        callback(validBody()).andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.error.code") { value("IDENTITY_PROVIDER_UNAVAILABLE") }
        }
    }

    @Test
    fun should_echo_request_id_when_client_sends_a_uuid() {
        every { identityProvider.authenticate(any()) } throws GoogleAuthenticationException("rejected")
        val requestId = UUID.randomUUID().toString()

        mockMvc.post(CALLBACK_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = validBody()
            header("X-Request-ID", requestId)
        }.andExpect {
            header { string("X-Request-ID", requestId) }
            jsonPath("$.meta.requestId") { value(requestId) }
        }
    }

    private fun callback(body: String): ResultActionsDsl = mockMvc.post(CALLBACK_PATH) {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun assertCookieAttributes(cookie: String) {
        listOf("ppp_refresh=", "Path=/api/v1/auth", "Max-Age=2592000", "Secure", "HttpOnly", "SameSite=Strict")
            .forEach { attribute -> assert(attribute in cookie) { "Missing '$attribute' in $cookie" } }
    }

    private fun cookieValue(cookie: String) = cookie.substringAfter("ppp_refresh=").substringBefore(";")

    private fun identityFor(email: String, name: String, subject: String = "google-${UUID.randomUUID()}") =
        VerifiedIdentity(subject, IdentityProfile(email, name, null))

    private fun authorization() = GoogleAuthorizationCode("auth-code", REDIRECT_URI, "verifier")

    private fun validBody(redirectUri: String = REDIRECT_URI) =
        """{"code":"auth-code","redirectUri":"$redirectUri","codeVerifier":"verifier"}"""

    private companion object {
        const val CALLBACK_PATH = "/api/v1/auth/google/callback"
        const val REDIRECT_URI = "http://localhost:3000/auth.html"
    }
}
