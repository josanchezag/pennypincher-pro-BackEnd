package com.pennypincher.pro.networth.infrastructure.persistence.entity

import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `assets` table; the shared `amount` attribute is the `value` column. */
@Entity
@Table(name = "assets")
@AttributeOverride(name = "amount", column = Column(name = "value", nullable = false))
class AssetEntity(
    id: UUID,
    userId: UUID,
    name: String,
    value: BigDecimal,
    category: String,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : CategorizedAmountEntity(id, userId, name, value, category, active, createdAt, updatedAt)
