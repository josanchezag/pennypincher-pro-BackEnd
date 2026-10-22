package com.pennypincher.pro.auth.infrastructure.persistence.adapter

import com.pennypincher.pro.auth.domain.model.RefreshToken
import com.pennypincher.pro.auth.domain.port.outbound.RefreshTokenRepository
import com.pennypincher.pro.auth.infrastructure.persistence.entity.RefreshTokenEntity
import com.pennypincher.pro.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Adapts Spring Data JPA to the domain [RefreshTokenRepository] port. */
@Component
class RefreshTokenRepositoryAdapter(private val jpaRepository: RefreshTokenJpaRepository) : RefreshTokenRepository {
    @Transactional
    override fun save(token: RefreshToken): RefreshToken = toDomain(jpaRepository.save(toEntity(token)))

    @Transactional(readOnly = true)
    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        jpaRepository.findByTokenHash(tokenHash)?.let(::toDomain)

    @Transactional
    override fun revoke(id: UUID, revokedAt: Instant): Boolean = jpaRepository.revokeIfActive(id, revokedAt) > 0

    @Transactional
    override fun deleteAllByUserId(userId: UUID): Int = jpaRepository.deleteAllTokensByUserId(userId)

    @Transactional
    override fun delete(id: UUID) = jpaRepository.deleteTokenById(id)

    private fun toEntity(token: RefreshToken): RefreshTokenEntity = RefreshTokenEntity(
        id = token.id,
        userId = token.userId,
        tokenHash = token.tokenHash,
        expiresAt = token.expiresAt,
        revoked = token.revoked,
        revokedAt = token.revokedAt,
        createdAt = token.createdAt,
    )

    private fun toDomain(entity: RefreshTokenEntity): RefreshToken = RefreshToken(
        id = entity.id,
        userId = entity.userId,
        tokenHash = entity.tokenHash,
        expiresAt = entity.expiresAt,
        revoked = entity.revoked,
        revokedAt = entity.revokedAt,
        createdAt = entity.createdAt,
    )
}
