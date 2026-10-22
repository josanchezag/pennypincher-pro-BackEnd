package com.pennypincher.pro.auth.domain.port.inbound

import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode

/** Sign-in with Google and the lifecycle of the issued tokens. */
interface AuthUseCase {
    fun authenticateWithGoogle(authorization: GoogleAuthorizationCode): AuthResult
}
