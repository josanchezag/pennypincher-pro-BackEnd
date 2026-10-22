package com.pennypincher.pro.expenses.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** A savings or investment vehicle with the amount the user sets aside each month and its current balance. */
data class SavingsAccount(
    override val id: UUID,
    override val userId: UUID,
    override val name: String,
    val monthlyContribution: BigDecimal,
    val currentBalance: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
) : BalanceAccount

/** Data a user supplies to create or update a savings account. */
data class SavingsAccountCommand(
    override val name: String,
    val monthlyContribution: BigDecimal,
    val currentBalance: BigDecimal,
) : BalanceAccountCommand
