package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.exception.ConcurrentRefreshException
import com.pennypincher.pro.auth.domain.exception.InvalidRefreshTokenException
import com.pennypincher.pro.auth.domain.model.AccessToken
import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.RefreshToken
import com.pennypincher.pro.auth.domain.port.outbound.RefreshTokenRepository
import com.pennypincher.pro.shared.domain.exception.ErrorCode
import com.pennypincher.pro.support.AuthFixtures
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RefreshTokenRotatorTest {
    private val repository = mockk<RefreshTokenRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val tokenPairIssuer = mockk<TokenPairIssuer>()
    private val codec = RefreshTokenCodec()
    private val now = AuthFixtures.NOW
    private val rotator =
        RefreshTokenRotator(repository, codec, userRepository, tokenPairIssuer, GRACE, Clock.fixed(now, ZoneOffset.UTC))
    private val user = AuthFixtures.user()

    @Test
    fun should_revoke_old_token_and_issue_new_pair_when_token_is_valid() {
        val stored = storedToken()
        val expected = AuthResult(AccessToken("access", Duration.ofMinutes(15)), "next", user)
        every { repository.revoke(stored.id, now) } returns true
        every { userRepository.findById(user.id) } returns user
        every { tokenPairIssuer.issueFor(user) } returns expected

        assertSame(expected, rotator.rotate(RAW_TOKEN))

        verify(exactly = 1) { repository.revoke(stored.id, now) }
    }

    @Test
    fun should_reject_and_revoke_whole_family_when_revoked_token_is_reused() {
        storedToken(revokedAt = now.minus(GRACE))

        assertThrows<InvalidRefreshTokenException> { rotator.rotate(RAW_TOKEN) }

        verify(exactly = 1) { repository.deleteAllByUserId(user.id) }
        verify(exactly = 0) { tokenPairIssuer.issueFor(any()) }
    }

    @Test
    fun should_keep_session_when_token_was_rotated_within_the_grace_period() {
        storedToken(revokedAt = now.minus(GRACE).plusSeconds(1))

        val superseded = assertThrows<ConcurrentRefreshException> { rotator.rotate(RAW_TOKEN) }
        assertEquals(ErrorCode.REFRESH_SUPERSEDED, superseded.errorCode)

        verify(exactly = 0) { repository.deleteAllByUserId(any()) }
    }

    @Test
    fun should_reject_token_when_it_is_expired() {
        storedToken(expiresAt = now)

        assertThrows<InvalidRefreshTokenException> { rotator.rotate(RAW_TOKEN) }

        verify(exactly = 0) { repository.revoke(any(), any()) }
    }

    @Test
    fun should_reject_token_when_cookie_is_missing_blank_or_unknown() {
        every { repository.findByTokenHash(any()) } returns null

        listOf(null, " ", "unknown").forEach { raw ->
            assertThrows<InvalidRefreshTokenException> { rotator.rotate(raw) }
        }
    }

    @Test
    fun should_reject_token_when_a_concurrent_refresh_revoked_it_first() {
        val stored = storedToken()
        every { repository.revoke(stored.id, now) } returns false

        assertThrows<ConcurrentRefreshException> { rotator.rotate(RAW_TOKEN) }

        verify(exactly = 0) { tokenPairIssuer.issueFor(any()) }
        verify(exactly = 0) { repository.deleteAllByUserId(any()) }
    }

    @Test
    fun should_reject_token_when_its_user_no_longer_exists() {
        val stored = storedToken()
        every { repository.revoke(stored.id, now) } returns true
        every { userRepository.findById(user.id) } returns null

        assertThrows<InvalidRefreshTokenException> { rotator.rotate(RAW_TOKEN) }
    }

    @Test
    fun should_delete_token_when_logging_out() {
        val stored = storedToken()

        rotator.revoke(RAW_TOKEN)

        verify(exactly = 1) { repository.delete(stored.id) }
    }

    @Test
    fun should_do_nothing_when_logging_out_with_missing_or_unknown_token() {
        rotator.revoke(null)
        every { repository.findByTokenHash(codec.hash("unknown")) } returns null
        rotator.revoke("unknown")

        verify(exactly = 0) { repository.delete(any()) }
        verify(exactly = 0) { repository.revoke(any(), any()) }
    }

    private fun storedToken(
        revokedAt: Instant? = null,
        expiresAt: Instant = now.plus(Duration.ofDays(1)),
    ): RefreshToken {
        val token = RefreshToken.issue(user.id, codec.hash(RAW_TOKEN), now.minus(Duration.ofDays(1)), expiresAt)
            .copy(revoked = revokedAt != null, revokedAt = revokedAt)
        every { repository.findByTokenHash(codec.hash(RAW_TOKEN)) } returns token
        return token
    }

    private companion object {
        const val RAW_TOKEN = "raw-refresh-token"
        val GRACE: Duration = Duration.ofSeconds(30)
    }
}
