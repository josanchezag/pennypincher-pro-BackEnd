package com.pennypincher.pro.auth.domain.port.inbound

import com.pennypincher.pro.auth.domain.model.AuthResult

/** Local-development sign-in for a fixed test user, without Google. */
interface DevLoginUseCase {
    fun authenticateDevUser(): AuthResult
}
