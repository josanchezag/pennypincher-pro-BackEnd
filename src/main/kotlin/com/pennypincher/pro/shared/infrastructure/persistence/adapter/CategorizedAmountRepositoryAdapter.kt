package com.pennypincher.pro.shared.infrastructure.persistence.adapter

import com.pennypincher.pro.shared.domain.port.outbound.UserScopedCrudRepository
import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import com.pennypincher.pro.shared.infrastructure.persistence.repository.CategorizedAmountJpaRepository
import java.math.BigDecimal
import java.util.UUID
import org.springframework.transaction.annotation.Transactional

/** Adapts a [CategorizedAmountJpaRepository] to the domain [UserScopedCrudRepository] port. */
@Transactional(readOnly = true)
abstract class CategorizedAmountRepositoryAdapter<T, E : CategorizedAmountEntity, C : Enum<C>>(
    private val jpaRepository: CategorizedAmountJpaRepository<E>,
) : UserScopedCrudRepository<T, C> {
    protected abstract fun toDomain(entity: E): T

    protected abstract fun toEntity(item: T): E

    override fun findAllActive(userId: UUID, category: C?): List<T> {
        val entities = if (category == null) {
            jpaRepository.findAllByUserIdAndActiveTrue(userId)
        } else {
            jpaRepository.findAllByUserIdAndCategoryAndActiveTrue(userId, category.name)
        }
        return entities.map(::toDomain)
    }

    override fun findActiveById(userId: UUID, id: UUID): T? =
        jpaRepository.findByIdAndUserIdAndActiveTrue(id, userId)?.let(::toDomain)

    @Transactional
    override fun save(item: T): T = toDomain(jpaRepository.save(toEntity(item)))

    @Transactional
    override fun deactivate(userId: UUID, id: UUID): Boolean = jpaRepository.deactivate(id, userId) > 0

    override fun sumActive(userId: UUID): BigDecimal = jpaRepository.sumActiveAmount(userId)
}
