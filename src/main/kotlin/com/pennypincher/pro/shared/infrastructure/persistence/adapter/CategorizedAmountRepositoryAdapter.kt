package com.pennypincher.pro.shared.infrastructure.persistence.adapter

import com.pennypincher.pro.shared.domain.port.outbound.SummableCrudRepository
import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import com.pennypincher.pro.shared.infrastructure.persistence.repository.CategorizedAmountJpaRepository
import java.math.BigDecimal
import java.util.UUID

/** Adapts a [CategorizedAmountJpaRepository] to the domain [SummableCrudRepository] port. */
abstract class CategorizedAmountRepositoryAdapter<T, E : CategorizedAmountEntity, C : Enum<C>>(
    private val jpaRepository: CategorizedAmountJpaRepository<E>,
) : OwnedRecordRepositoryAdapter<T, E, C>(jpaRepository),
    SummableCrudRepository<T, C> {
    override fun findAllActive(userId: UUID, category: C?): List<T> = if (category == null) {
        allActive(userId)
    } else {
        jpaRepository.findAllByUserIdAndCategoryAndActiveTrue(userId, category.name).map(::toDomain)
    }

    override fun sumActive(userId: UUID): BigDecimal = jpaRepository.sumActiveAmount(userId)
}
