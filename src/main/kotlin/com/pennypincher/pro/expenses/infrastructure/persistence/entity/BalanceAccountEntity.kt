package com.pennypincher.pro.expenses.infrastructure.persistence.entity

import com.pennypincher.pro.shared.infrastructure.persistence.entity.OwnedRecordEntity
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Columns of the "name, monthly amount, balance" tables (credit cards, loans, savings accounts) on top of
 * [OwnedRecordEntity]. Subclasses map `monthlyAmount` and `balance` to their columns with `@AttributeOverride`.
 */
@MappedSuperclass
abstract class BalanceAccountEntity(
    id: UUID,
    userId: UUID,
    name: String,
    @Column(name = "monthly_amount", nullable = false)
    var monthlyAmount: BigDecimal,
    @Column(name = "balance", nullable = false)
    var balance: BigDecimal,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : OwnedRecordEntity(id, userId, name, active, createdAt, updatedAt)
