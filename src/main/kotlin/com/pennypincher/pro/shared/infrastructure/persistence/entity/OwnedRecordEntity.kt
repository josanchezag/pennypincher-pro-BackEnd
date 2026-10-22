package com.pennypincher.pro.shared.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.Instant
import java.util.UUID

/**
 * Columns shared by every soft-deletable, per-user record table. `is_active` is written only on insert;
 * afterwards only the soft-delete query changes it, so saving a record that was deleted in the meantime
 * cannot bring it back.
 */
@MappedSuperclass
abstract class OwnedRecordEntity(
    @Id
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "is_active", nullable = false, updatable = false)
    val active: Boolean,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
