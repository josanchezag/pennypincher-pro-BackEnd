package com.pennypincher.pro.expenses.infrastructure.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `savings_accounts` table: the monthly amount is the contribution. */
@Entity
@Table(name = "savings_accounts")
@AttributeOverride(name = "monthlyAmount", column = Column(name = "monthly_contribution", nullable = false))
@AttributeOverride(name = "balance", column = Column(name = "current_balance", nullable = false))
class SavingsAccountEntity(
    id: UUID,
    userId: UUID,
    name: String,
    monthlyContribution: BigDecimal,
    currentBalance: BigDecimal,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : BalanceAccountEntity(id, userId, name, monthlyContribution, currentBalance, active, createdAt, updatedAt)
