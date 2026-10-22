package com.pennypincher.pro.auth.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `refresh_tokens` table. */
@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "token_hash", nullable = false, updatable = false)
    val tokenHash: String,
    @Column(name = "expires_at", nullable = false, updatable = false)
    val expiresAt: Instant,
    @Column(name = "revoked", nullable = false)
    var revoked: Boolean,
    @Column(name = "revoked_at")
    var revokedAt: Instant?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
)
