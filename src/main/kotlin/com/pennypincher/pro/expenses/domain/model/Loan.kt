package com.pennypincher.pro.expenses.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * A bank or personal loan with its fixed monthly installment and remaining balance, in COP. The optional
 * [originalAmount] (the principal when the loan started) only serves to show repayment progress; it is
 * not required to be at least the balance.
 */
data class Loan(
    override val id: UUID,
    override val userId: UUID,
    override val name: String,
    val monthlyPayment: BigDecimal,
    val totalBalance: BigDecimal,
    val originalAmount: BigDecimal?,
    val createdAt: Instant,
    val updatedAt: Instant,
) : BalanceAccount

/** Data a user supplies to create or update a loan; a null [originalAmount] means it is not tracked. */
data class LoanCommand(
    override val name: String,
    val monthlyPayment: BigDecimal,
    val totalBalance: BigDecimal,
    val originalAmount: BigDecimal?,
) : BalanceAccountCommand
