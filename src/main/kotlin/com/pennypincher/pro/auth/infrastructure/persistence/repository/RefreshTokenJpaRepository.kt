package com.pennypincher.pro.auth.infrastructure.persistence.repository

import com.pennypincher.pro.auth.infrastructure.persistence.entity.RefreshTokenEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, UUID>
