package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.RefreshToken
import com.pennypincher.pro.auth.domain.port.outbound.AccessTokenIssuer
import com.pennypincher.pro.auth.domain.port.outbound.RefreshTokenRepository
import com.pennypincher.pro.user.domain.model.User
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Issues a fresh access token and a new stored refresh token for a user. */
class TokenPairIssuer(
    private val accessTokenIssuer: AccessTokenIssuer,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val codec: RefreshTokenCodec,
    private val refreshTokenTtl: Duration,
    private val clock: Clock,
) {
    fun issueFor(user: User): AuthResult {
        val now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS)
        val refreshToken = codec.generate()
        refreshTokenRepository.save(
            RefreshToken.issue(user.id, codec.hash(refreshToken), now, now.plus(refreshTokenTtl)),
        )
        return AuthResult(accessTokenIssuer.issue(user), refreshToken, user)
    }
}
