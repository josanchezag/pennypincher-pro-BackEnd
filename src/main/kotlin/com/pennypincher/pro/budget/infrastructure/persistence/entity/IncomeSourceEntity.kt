package com.pennypincher.pro.budget.infrastructure.persistence.entity

import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `income_sources` table; adds the reference `currency` to the shared columns. */
@Entity
@Table(name = "income_sources")
class IncomeSourceEntity(
    id: UUID,
    userId: UUID,
    name: String,
    amount: BigDecimal,
    category: String,
    @Column(name = "currency", nullable = false)
    var currency: String,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : CategorizedAmountEntity(id, userId, name, amount, category, active, createdAt, updatedAt)
