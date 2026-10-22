package com.pennypincher.pro.auth.domain.port.outbound

import com.pennypincher.pro.auth.domain.model.RefreshToken
import java.time.Instant
import java.util.UUID

/**
 * Persistence port for refresh tokens. Lookup is by token hash because the hash itself identifies
 * the owner; revocation is atomic so two concurrent refreshes cannot both succeed. A token is only
 * marked revoked by rotation; logout and reuse detection delete tokens instead.
 */
interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken

    fun findByTokenHash(tokenHash: String): RefreshToken?

    /** Revokes the token if it is still active; returns false when it was already revoked. */
    fun revoke(id: UUID, revokedAt: Instant): Boolean

    /** Removes every token of the user (reuse detected); returns how many were removed. */
    fun deleteAllByUserId(userId: UUID): Int

    /** Removes the token (logout). Deleting an already removed token does nothing. */
    fun delete(id: UUID)
}
