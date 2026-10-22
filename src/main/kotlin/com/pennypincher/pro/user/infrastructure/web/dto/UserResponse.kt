package com.pennypincher.pro.user.infrastructure.web.dto

import java.time.Instant
import java.util.UUID

/** Public representation of a user profile. */
data class UserResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val pictureUrl: String?,
    val createdAt: Instant,
)
