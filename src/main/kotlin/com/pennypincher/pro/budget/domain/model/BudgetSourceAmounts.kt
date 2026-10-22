package com.pennypincher.pro.budget.domain.model

import java.math.BigDecimal

/**
 * Sums over the user's active income, debt, expense and savings records, read together at one moment so
 * that a budget is calculated from one consistent picture. Every sum is zero when there is no record.
 */
data class BudgetSourceAmounts(
    val totalIncome: BigDecimal,
    val creditCardPayments: BigDecimal,
    val loanPayments: BigDecimal,
    /** Fixed expenses of every category except charity. */
    val nonCharityFixedExpenses: BigDecimal,
    /** Fixed expenses of the charity category. */
    val charityFixedExpenses: BigDecimal,
    val variableExpenses: BigDecimal,
    val savingsContributions: BigDecimal,
)
