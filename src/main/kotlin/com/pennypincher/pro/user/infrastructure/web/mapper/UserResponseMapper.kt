package com.pennypincher.pro.user.infrastructure.web.mapper

import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.infrastructure.web.dto.UserResponse

/** Builds the public user representation from the domain model. */
object UserResponseMapper {
    fun toResponse(user: User): UserResponse = UserResponse(
        id = user.id,
        email = user.email,
        name = user.name,
        pictureUrl = user.pictureUrl,
        createdAt = user.createdAt,
    )
}
