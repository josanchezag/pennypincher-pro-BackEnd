package com.pennypincher.pro.expenses.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** A credit card with the payment the user plans to make each month and its outstanding balance, in COP. */
data class CreditCard(
    override val id: UUID,
    override val userId: UUID,
    override val name: String,
    val monthlyPayment: BigDecimal,
    val totalBalance: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
) : BalanceAccount

/** Data a user supplies to create or update a credit card. */
data class CreditCardCommand(override val name: String, val monthlyPayment: BigDecimal, val totalBalance: BigDecimal) :
    BalanceAccountCommand
