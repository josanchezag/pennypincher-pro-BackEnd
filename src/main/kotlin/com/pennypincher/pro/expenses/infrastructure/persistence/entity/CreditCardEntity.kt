package com.pennypincher.pro.expenses.infrastructure.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `credit_cards` table: the monthly amount is the payment, the balance the total owed. */
@Entity
@Table(name = "credit_cards")
@AttributeOverride(name = "monthlyAmount", column = Column(name = "monthly_payment", nullable = false))
@AttributeOverride(name = "balance", column = Column(name = "total_balance", nullable = false))
class CreditCardEntity(
    id: UUID,
    userId: UUID,
    name: String,
    monthlyPayment: BigDecimal,
    totalBalance: BigDecimal,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : BalanceAccountEntity(id, userId, name, monthlyPayment, totalBalance, active, createdAt, updatedAt)
