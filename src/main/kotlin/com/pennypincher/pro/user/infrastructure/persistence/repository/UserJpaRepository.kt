package com.pennypincher.pro.user.infrastructure.persistence.repository

import com.pennypincher.pro.user.infrastructure.persistence.entity.UserEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, UUID> {
    fun findByGoogleSub(googleSub: String): UserEntity?
}
