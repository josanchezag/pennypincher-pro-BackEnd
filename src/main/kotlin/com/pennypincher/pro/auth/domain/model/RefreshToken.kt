package com.pennypincher.pro.auth.domain.model

import java.time.Instant
import java.util.UUID

/** A stored refresh token. Only the SHA-256 hash of the opaque value sent to the browser is kept. */
data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val revoked: Boolean,
    val createdAt: Instant,
) {
    fun isExpiredAt(now: Instant): Boolean = !now.isBefore(expiresAt)

    companion object {
        fun issue(userId: UUID, tokenHash: String, now: Instant, expiresAt: Instant): RefreshToken = RefreshToken(
            id = UUID.randomUUID(),
            userId = userId,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
            revoked = false,
            createdAt = now,
        )
    }
}
