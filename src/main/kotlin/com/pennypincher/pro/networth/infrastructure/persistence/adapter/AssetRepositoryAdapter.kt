package com.pennypincher.pro.networth.infrastructure.persistence.adapter

import com.pennypincher.pro.networth.domain.model.Asset
import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.networth.domain.port.outbound.AssetRepository
import com.pennypincher.pro.networth.infrastructure.persistence.entity.AssetEntity
import com.pennypincher.pro.networth.infrastructure.persistence.repository.AssetJpaRepository
import com.pennypincher.pro.shared.infrastructure.persistence.adapter.CategorizedAmountRepositoryAdapter
import org.springframework.stereotype.Component

/** Adapts Spring Data JPA to the domain [AssetRepository] port. Only active assets are ever read. */
@Component
class AssetRepositoryAdapter(jpaRepository: AssetJpaRepository) :
    CategorizedAmountRepositoryAdapter<Asset, AssetEntity, AssetCategory>(jpaRepository),
    AssetRepository {
    override fun toDomain(entity: AssetEntity) = Asset(
        id = entity.id,
        userId = entity.userId,
        name = entity.name,
        value = entity.amount,
        category = AssetCategory.valueOf(entity.category),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(item: Asset) = AssetEntity(
        id = item.id,
        userId = item.userId,
        name = item.name,
        value = item.value,
        category = item.category.name,
        active = true,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
