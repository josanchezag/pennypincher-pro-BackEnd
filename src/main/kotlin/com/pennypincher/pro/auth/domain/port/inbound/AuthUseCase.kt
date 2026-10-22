package com.pennypincher.pro.auth.domain.port.inbound

import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode

/** Sign-in with Google and the lifecycle of the issued tokens. */
interface AuthUseCase {
    fun authenticateWithGoogle(authorization: GoogleAuthorizationCode): AuthResult

    /** Rotates the refresh token: revokes the presented one and issues a new pair. */
    fun refreshToken(refreshToken: String?): AuthResult

    /** Revokes the refresh token if it is still active. Missing or unknown tokens are ignored. */
    fun revokeRefreshToken(refreshToken: String?)
}
