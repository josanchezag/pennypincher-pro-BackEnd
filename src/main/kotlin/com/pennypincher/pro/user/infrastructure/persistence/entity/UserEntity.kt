package com.pennypincher.pro.user.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `users` table. `updated_at` is maintained by a database trigger. */
@Entity
@Table(name = "users")
class UserEntity(
    @Id
    val id: UUID,
    @Column(name = "google_sub", nullable = false, updatable = false)
    val googleSub: String,
    @Column(name = "email", nullable = false)
    var email: String,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "picture_url")
    var pictureUrl: String?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
)
