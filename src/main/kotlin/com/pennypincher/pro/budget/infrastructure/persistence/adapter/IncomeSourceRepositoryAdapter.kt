package com.pennypincher.pro.budget.infrastructure.persistence.adapter

import com.pennypincher.pro.budget.domain.model.IncomeSource
import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import com.pennypincher.pro.budget.domain.port.outbound.IncomeSourceRepository
import com.pennypincher.pro.budget.infrastructure.persistence.entity.IncomeSourceEntity
import com.pennypincher.pro.budget.infrastructure.persistence.repository.IncomeSourceJpaRepository
import com.pennypincher.pro.shared.infrastructure.persistence.adapter.CategorizedAmountRepositoryAdapter
import org.springframework.stereotype.Component

/** Adapts Spring Data JPA to the domain [IncomeSourceRepository] port. Only active sources are ever read. */
@Component
class IncomeSourceRepositoryAdapter(jpaRepository: IncomeSourceJpaRepository) :
    CategorizedAmountRepositoryAdapter<IncomeSource, IncomeSourceEntity, IncomeCategory>(jpaRepository),
    IncomeSourceRepository {
    override fun toDomain(entity: IncomeSourceEntity) = IncomeSource(
        id = entity.id,
        userId = entity.userId,
        name = entity.name,
        category = IncomeCategory.valueOf(entity.category),
        amount = entity.amount,
        currency = entity.currency,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(item: IncomeSource) = IncomeSourceEntity(
        id = item.id,
        userId = item.userId,
        name = item.name,
        amount = item.amount,
        category = item.category.name,
        currency = item.currency,
        active = true,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
