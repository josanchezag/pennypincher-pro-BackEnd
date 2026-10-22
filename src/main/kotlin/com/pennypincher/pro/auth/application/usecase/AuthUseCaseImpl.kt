package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode
import com.pennypincher.pro.auth.domain.port.inbound.AuthUseCase
import com.pennypincher.pro.auth.domain.port.outbound.IdentityProviderPort
import com.pennypincher.pro.shared.domain.exception.ValidationException

/**
 * Google sign-in (checks the redirect URI, verifies the identity, upserts the user and issues tokens)
 * plus refresh token rotation and revocation.
 */
class AuthUseCaseImpl(
    private val identityProvider: IdentityProviderPort,
    private val userUpserter: UserUpserter,
    private val tokenPairIssuer: TokenPairIssuer,
    private val refreshTokenRotator: RefreshTokenRotator,
    private val allowedRedirectUris: Set<String>,
) : AuthUseCase {
    override fun authenticateWithGoogle(authorization: GoogleAuthorizationCode): AuthResult {
        if (authorization.redirectUri !in allowedRedirectUris) {
            throw ValidationException(REDIRECT_URI_FIELD, "is not an allowed redirect URI")
        }
        val identity = identityProvider.authenticate(authorization)
        return tokenPairIssuer.issueFor(userUpserter.upsert(identity))
    }

    override fun refreshToken(refreshToken: String?): AuthResult = refreshTokenRotator.rotate(refreshToken)

    override fun revokeRefreshToken(refreshToken: String?) = refreshTokenRotator.revoke(refreshToken)

    private companion object {
        const val REDIRECT_URI_FIELD = "redirectUri"
    }
}
