package com.pennypincher.pro.auth.infrastructure.persistence.repository

import com.pennypincher.pro.auth.infrastructure.persistence.entity.RefreshTokenEntity
import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    @Modifying
    @Query(
        "update RefreshTokenEntity t set t.revoked = true, t.revokedAt = :revokedAt " +
            "where t.id = :id and t.revoked = false",
    )
    fun revokeIfActive(@Param("id") id: UUID, @Param("revokedAt") revokedAt: Instant): Int

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.userId = :userId")
    fun deleteAllTokensByUserId(@Param("userId") userId: UUID): Int

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.id = :id")
    fun deleteTokenById(@Param("id") id: UUID)
}
