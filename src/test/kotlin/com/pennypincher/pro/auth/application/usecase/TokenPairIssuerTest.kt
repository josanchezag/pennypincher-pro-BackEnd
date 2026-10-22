package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.model.AccessToken
import com.pennypincher.pro.auth.domain.model.RefreshToken
import com.pennypincher.pro.auth.domain.port.outbound.AccessTokenIssuer
import com.pennypincher.pro.auth.domain.port.outbound.RefreshTokenRepository
import com.pennypincher.pro.support.AuthFixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TokenPairIssuerTest {
    private val accessTokenIssuer = mockk<AccessTokenIssuer>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val codec = RefreshTokenCodec()
    private val issuer =
        TokenPairIssuer(
            accessTokenIssuer,
            refreshTokenRepository,
            codec,
            Duration.ofDays(30),
            Clock.fixed(AuthFixtures.NOW, ZoneOffset.UTC),
        )

    @Test
    fun should_store_only_the_hash_when_issuing_a_refresh_token() {
        val user = AuthFixtures.user()
        val stored = slot<RefreshToken>()
        every { accessTokenIssuer.issue(user) } returns AccessToken("access", Duration.ofMinutes(15))
        every { refreshTokenRepository.save(capture(stored)) } answers { stored.captured }

        val result = issuer.issueFor(user)

        assertEquals("access", result.accessToken.value)
        assertEquals(user, result.user)
        assertEquals(codec.hash(result.refreshToken), stored.captured.tokenHash)
        assertEquals(user.id, stored.captured.userId)
        assertEquals(AuthFixtures.NOW.plus(Duration.ofDays(30)), stored.captured.expiresAt)
        assertFalse(stored.captured.revoked)
    }
}
