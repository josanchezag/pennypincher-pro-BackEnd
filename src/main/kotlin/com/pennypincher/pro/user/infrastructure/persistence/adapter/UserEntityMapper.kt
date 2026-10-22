package com.pennypincher.pro.user.infrastructure.persistence.adapter

import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.infrastructure.persistence.entity.UserEntity

/** Converts between the domain user and its JPA entity. */
object UserEntityMapper {
    fun toDomain(entity: UserEntity): User = User(
        id = entity.id,
        googleSub = entity.googleSub,
        email = entity.email,
        name = entity.name,
        pictureUrl = entity.pictureUrl,
        createdAt = entity.createdAt,
    )

    fun toEntity(user: User): UserEntity = UserEntity(
        id = user.id,
        googleSub = user.googleSub,
        email = user.email,
        name = user.name,
        pictureUrl = user.pictureUrl,
        createdAt = user.createdAt,
    )
}
