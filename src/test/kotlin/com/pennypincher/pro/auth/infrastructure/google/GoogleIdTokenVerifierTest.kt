package com.pennypincher.pro.auth.infrastructure.google

import com.pennypincher.pro.auth.domain.exception.GoogleAuthenticationException
import com.pennypincher.pro.auth.domain.exception.IdentityProviderUnavailableException
import com.pennypincher.pro.support.GoogleIdTokens
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

class GoogleIdTokenVerifierTest {
    private val tokens = GoogleIdTokens()
    private val verifier = GoogleIdTokenVerifier(decoderFor(tokens))

    @Test
    fun should_return_identity_when_token_is_valid() {
        val identity = verifier.verify(tokens.sign())

        assertEquals("google-sub-123", identity.subject)
        assertEquals("ana@example.com", identity.profile.email)
        assertEquals("Ana Pérez", identity.profile.name)
        assertEquals("https://lh3.googleusercontent.com/a/ana", identity.profile.pictureUrl)
    }

    @Test
    fun should_accept_issuer_without_scheme_when_google_uses_short_form() {
        val identity = verifier.verify(tokens.sign(GoogleIdTokens.Claims(issuer = "accounts.google.com")))

        assertEquals("google-sub-123", identity.subject)
    }

    @Test
    fun should_use_email_as_name_when_name_claim_is_missing() {
        val identity = verifier.verify(tokens.sign(GoogleIdTokens.Claims(name = null, picture = null)))

        assertEquals("ana@example.com", identity.profile.name)
        assertNull(identity.profile.pictureUrl)
    }

    @Test
    fun should_truncate_name_when_google_name_is_longer_than_allowed() {
        val identity = verifier.verify(tokens.sign(GoogleIdTokens.Claims(name = "n".repeat(300))))

        assertEquals(100, identity.profile.name.length)
    }

    @Test
    fun should_reject_token_when_it_is_expired() {
        val expired = GoogleIdTokens.Claims(issuedAt = Instant.now().minus(Duration.ofHours(3)))

        assertThrows<GoogleAuthenticationException> { verifier.verify(tokens.sign(expired)) }
    }

    @Test
    fun should_reject_token_when_audience_is_another_client() {
        val claims = GoogleIdTokens.Claims(audience = "someone-else.apps.googleusercontent.com")

        assertThrows<GoogleAuthenticationException> { verifier.verify(tokens.sign(claims)) }
    }

    @Test
    fun should_reject_token_when_issuer_is_not_google() {
        val claims = GoogleIdTokens.Claims(issuer = "https://evil.example.com")

        assertThrows<GoogleAuthenticationException> { verifier.verify(tokens.sign(claims)) }
    }

    @Test
    fun should_reject_token_when_email_is_not_verified() {
        assertThrows<GoogleAuthenticationException> {
            verifier.verify(tokens.sign(GoogleIdTokens.Claims(emailVerified = false)))
        }
        assertThrows<GoogleAuthenticationException> {
            verifier.verify(tokens.sign(GoogleIdTokens.Claims(emailVerified = null)))
        }
    }

    @Test
    fun should_reject_token_when_email_or_subject_is_missing() {
        assertThrows<GoogleAuthenticationException> {
            verifier.verify(tokens.sign(GoogleIdTokens.Claims(email = null)))
        }
        assertThrows<GoogleAuthenticationException> {
            verifier.verify(tokens.sign(GoogleIdTokens.Claims(subject = null)))
        }
    }

    @Test
    fun should_reject_token_when_signed_by_another_key() {
        val forged = GoogleIdTokens().sign()

        assertThrows<GoogleAuthenticationException> { verifier.verify(forged) }
    }

    @Test
    fun should_reject_token_when_it_is_not_a_jwt() {
        assertThrows<GoogleAuthenticationException> { verifier.verify("not-a-jwt") }
    }

    @Test
    fun should_report_unavailable_when_signing_keys_cannot_be_fetched() {
        val failingDecoder = mockk<JwtDecoder>()
        every { failingDecoder.decode(any()) } throws JwtException("Couldn't retrieve remote JWK set")

        assertThrows<IdentityProviderUnavailableException> { GoogleIdTokenVerifier(failingDecoder).verify("token") }
    }

    private fun decoderFor(tokens: GoogleIdTokens): JwtDecoder = NimbusJwtDecoder.withPublicKey(tokens.publicKey)
        .build()
        .apply { setJwtValidator(GoogleIdTokenValidators.create(GoogleIdTokens.CLIENT_ID)) }
}
