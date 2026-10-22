package com.pennypincher.pro.auth.domain.model

import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A stored refresh token. Only the SHA-256 hash of the opaque value sent to the browser is kept.
 * A token is revoked when it is rotated; logging out deletes it.
 */
data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val revoked: Boolean,
    val revokedAt: Instant?,
    val createdAt: Instant,
) {
    fun isExpiredAt(now: Instant): Boolean = !now.isBefore(expiresAt)

    /** True when the token was revoked less than [grace] before [now], as happens to concurrent refreshes. */
    fun wasRevokedWithin(grace: Duration, now: Instant): Boolean =
        revoked && revokedAt != null && now.isBefore(revokedAt.plus(grace))

    companion object {
        fun issue(userId: UUID, tokenHash: String, now: Instant, expiresAt: Instant): RefreshToken = RefreshToken(
            id = UUID.randomUUID(),
            userId = userId,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
            revoked = false,
            revokedAt = null,
            createdAt = now,
        )
    }
}
