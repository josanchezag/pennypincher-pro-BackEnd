package com.pennypincher.pro.expenses.infrastructure.persistence.entity

import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `fixed_expenses` table; the shared `amount` attribute is the `monthly_amount` column. */
@Entity
@Table(name = "fixed_expenses")
@AttributeOverride(name = "amount", column = Column(name = "monthly_amount", nullable = false))
class FixedExpenseEntity(
    id: UUID,
    userId: UUID,
    name: String,
    monthlyAmount: BigDecimal,
    category: String,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : CategorizedAmountEntity(id, userId, name, monthlyAmount, category, active, createdAt, updatedAt)
