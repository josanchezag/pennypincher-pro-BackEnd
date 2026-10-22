package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.exception.GoogleAuthenticationException
import com.pennypincher.pro.auth.domain.model.AccessToken
import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode
import com.pennypincher.pro.auth.domain.port.outbound.IdentityProviderPort
import com.pennypincher.pro.shared.domain.exception.ValidationException
import com.pennypincher.pro.support.AuthFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthUseCaseImplTest {
    private val identityProvider = mockk<IdentityProviderPort>()
    private val userUpserter = mockk<UserUpserter>()
    private val tokenPairIssuer = mockk<TokenPairIssuer>()
    private val rotator = mockk<RefreshTokenRotator>(relaxed = true)
    private val useCase = AuthUseCaseImpl(identityProvider, userUpserter, tokenPairIssuer, rotator, setOf(REDIRECT_URI))
    private val authorization = GoogleAuthorizationCode("code", REDIRECT_URI, "verifier")

    @Test
    fun should_issue_tokens_for_upserted_user_when_google_identity_is_valid() {
        val user = AuthFixtures.user()
        val expected = AuthResult(AccessToken("access", Duration.ofMinutes(15)), "refresh", user)
        every { identityProvider.authenticate(authorization) } returns AuthFixtures.IDENTITY
        every { userUpserter.upsert(AuthFixtures.IDENTITY) } returns user
        every { tokenPairIssuer.issueFor(user) } returns expected

        assertSame(expected, useCase.authenticateWithGoogle(authorization))
    }

    @Test
    fun should_reject_request_without_calling_google_when_redirect_uri_is_not_allowed() {
        val exception = assertThrows<ValidationException> {
            useCase.authenticateWithGoogle(authorization.copy(redirectUri = "https://evil.example.com/cb"))
        }

        assertEquals("redirectUri", exception.violations.single().field)
        verify(exactly = 0) { identityProvider.authenticate(any()) }
    }

    @Test
    fun should_not_touch_users_when_google_rejects_the_code() {
        every { identityProvider.authenticate(authorization) } throws GoogleAuthenticationException("rejected")

        assertThrows<GoogleAuthenticationException> { useCase.authenticateWithGoogle(authorization) }
        verify(exactly = 0) { userUpserter.upsert(any()) }
    }

    @Test
    fun should_delegate_to_rotator_when_refreshing_or_revoking() {
        val expected = AuthResult(AccessToken("access", Duration.ofMinutes(15)), "next", AuthFixtures.user())
        every { rotator.rotate("current") } returns expected

        assertSame(expected, useCase.refreshToken("current"))
        useCase.revokeRefreshToken("current")

        verify { rotator.revoke("current") }
    }

    private companion object {
        const val REDIRECT_URI = "http://localhost:3000/auth.html"
    }
}
