package com.pennypincher.pro.budget.domain.model.enums

import java.math.BigDecimal

private val CREDIT_CARD_IDEAL_PERCENTAGE = BigDecimal("20.00")
private val SAVINGS_IDEAL_PERCENTAGE = BigDecimal("10.00")
private val CHARITY_IDEAL_PERCENTAGE = BigDecimal("10.00")

/**
 * Where the monthly income goes. [idealPercentage] is the target share of the income shown for comparison
 * (null when there is none); [countsAsExpense] is false only for savings, which are not part of the total
 * expenses. The declaration order is the order of the budget breakdown.
 */
enum class BudgetComponent(val idealPercentage: BigDecimal?, val countsAsExpense: Boolean) {
    CREDIT_CARDS(CREDIT_CARD_IDEAL_PERCENTAGE, true),
    LOANS(null, true),
    MONTHLY_EXPENSES(null, true),
    SAVINGS(SAVINGS_IDEAL_PERCENTAGE, false),
    CHARITY(CHARITY_IDEAL_PERCENTAGE, true),
}
