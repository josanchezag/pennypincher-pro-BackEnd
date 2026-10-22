package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.exception.ConcurrentRefreshException
import com.pennypincher.pro.auth.domain.exception.InvalidRefreshTokenException
import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.RefreshToken
import com.pennypincher.pro.auth.domain.port.outbound.RefreshTokenRepository
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Refresh token rotation with reuse detection. A token revoked by a rotation less than [reuseGrace]
 * ago is a concurrent refresh: it is rejected without touching the session. Presented later, it means
 * the token leaked or was replayed, so every token of that user is deleted (all sessions end).
 */
class RefreshTokenRotator(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val codec: RefreshTokenCodec,
    private val userRepository: UserRepository,
    private val tokenPairIssuer: TokenPairIssuer,
    private val reuseGrace: Duration,
    private val clock: Clock,
) {
    fun rotate(rawToken: String?): AuthResult {
        val token = find(rawToken) ?: throw InvalidRefreshTokenException()
        val now = Instant.now(clock)
        when {
            token.revoked -> rejectRevoked(token, now)
            token.isExpiredAt(now) -> throw InvalidRefreshTokenException()
            !refreshTokenRepository.revoke(token.id, now) -> throw ConcurrentRefreshException()
        }
        val user = userRepository.findById(token.userId) ?: throw InvalidRefreshTokenException()
        return tokenPairIssuer.issueFor(user)
    }

    /** Logout: deletes the token, so presenting it again is simply unknown and affects no other device. */
    fun revoke(rawToken: String?) {
        find(rawToken)?.let { refreshTokenRepository.delete(it.id) }
    }

    private fun rejectRevoked(token: RefreshToken, now: Instant): Nothing {
        if (token.wasRevokedWithin(reuseGrace, now)) {
            throw ConcurrentRefreshException()
        }
        refreshTokenRepository.deleteAllByUserId(token.userId)
        throw InvalidRefreshTokenException()
    }

    private fun find(rawToken: String?): RefreshToken? =
        rawToken?.takeIf(String::isNotBlank)?.let { refreshTokenRepository.findByTokenHash(codec.hash(it)) }
}
