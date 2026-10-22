package com.pennypincher.pro.expenses.infrastructure.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * JPA mapping of the `loans` table: the monthly amount is the installment, the balance the remaining
 * principal; adds the optional `original_amount`.
 */
@Entity
@Table(name = "loans")
@AttributeOverride(name = "monthlyAmount", column = Column(name = "monthly_payment", nullable = false))
@AttributeOverride(name = "balance", column = Column(name = "total_balance", nullable = false))
class LoanEntity(
    id: UUID,
    userId: UUID,
    name: String,
    monthlyPayment: BigDecimal,
    totalBalance: BigDecimal,
    @Column(name = "original_amount")
    var originalAmount: BigDecimal?,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : BalanceAccountEntity(id, userId, name, monthlyPayment, totalBalance, active, createdAt, updatedAt)
