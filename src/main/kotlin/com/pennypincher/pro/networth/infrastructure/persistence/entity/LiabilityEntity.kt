package com.pennypincher.pro.networth.infrastructure.persistence.entity

import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `liabilities` table; the shared `amount` attribute is the `current_balance` column. */
@Entity
@Table(name = "liabilities")
@AttributeOverride(name = "amount", column = Column(name = "current_balance", nullable = false))
class LiabilityEntity(
    id: UUID,
    userId: UUID,
    name: String,
    currentBalance: BigDecimal,
    category: String,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : CategorizedAmountEntity(id, userId, name, currentBalance, category, active, createdAt, updatedAt)
