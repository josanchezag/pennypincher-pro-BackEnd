package com.pennypincher.pro.networth.infrastructure.persistence.adapter

import com.pennypincher.pro.networth.domain.model.Liability
import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import com.pennypincher.pro.networth.domain.port.outbound.LiabilityRepository
import com.pennypincher.pro.networth.infrastructure.persistence.entity.LiabilityEntity
import com.pennypincher.pro.networth.infrastructure.persistence.repository.LiabilityJpaRepository
import com.pennypincher.pro.shared.infrastructure.persistence.adapter.CategorizedAmountRepositoryAdapter
import org.springframework.stereotype.Component

/** Adapts Spring Data JPA to the domain [LiabilityRepository] port. Only active liabilities are ever read. */
@Component
class LiabilityRepositoryAdapter(jpaRepository: LiabilityJpaRepository) :
    CategorizedAmountRepositoryAdapter<Liability, LiabilityEntity, LiabilityCategory>(jpaRepository),
    LiabilityRepository {
    override fun toDomain(entity: LiabilityEntity) = Liability(
        id = entity.id,
        userId = entity.userId,
        name = entity.name,
        currentBalance = entity.amount,
        category = LiabilityCategory.valueOf(entity.category),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(item: Liability) = LiabilityEntity(
        id = item.id,
        userId = item.userId,
        name = item.name,
        currentBalance = item.currentBalance,
        category = item.category.name,
        active = true,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
