package com.pennypincher.pro.shared.infrastructure.persistence.adapter

import com.pennypincher.pro.shared.domain.port.outbound.UserScopedCrudRepository
import com.pennypincher.pro.shared.infrastructure.persistence.entity.OwnedRecordEntity
import com.pennypincher.pro.shared.infrastructure.persistence.repository.OwnedRecordJpaRepository
import java.util.UUID
import org.springframework.transaction.annotation.Transactional

/**
 * Adapts an [OwnedRecordJpaRepository] to the domain [UserScopedCrudRepository] port. Subclasses map
 * between the entity and the domain and decide how the list is filtered.
 */
@Transactional(readOnly = true)
abstract class OwnedRecordRepositoryAdapter<T, E : OwnedRecordEntity, C>(
    private val jpaRepository: OwnedRecordJpaRepository<E>,
) : UserScopedCrudRepository<T, C> {
    protected abstract fun toDomain(entity: E): T

    protected abstract fun toEntity(item: T): E

    /** Every active record of the user, in no particular order. */
    protected fun allActive(userId: UUID): List<T> = jpaRepository.findAllByUserIdAndActiveTrue(userId).map(::toDomain)

    override fun findActiveById(userId: UUID, id: UUID): T? =
        jpaRepository.findByIdAndUserIdAndActiveTrue(id, userId)?.let(::toDomain)

    @Transactional
    override fun save(item: T): T = toDomain(jpaRepository.save(toEntity(item)))

    @Transactional
    override fun deactivate(userId: UUID, id: UUID): Boolean = jpaRepository.deactivate(id, userId) > 0
}
