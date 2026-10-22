package com.pennypincher.pro.auth.domain.port.outbound

import com.pennypincher.pro.auth.domain.model.RefreshToken

/** Persistence port for refresh tokens. */
interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken
}
