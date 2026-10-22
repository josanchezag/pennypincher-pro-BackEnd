package com.pennypincher.pro.budget.domain.model

import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import java.math.BigDecimal

/** Monthly amount of one budget component and its share of the income ([percentage] has 4 decimals). */
data class BudgetComponentLine(val component: BudgetComponent, val amount: BigDecimal, val percentage: BigDecimal)

/** Calculated totals of a budget, with one line per [BudgetComponent] in declaration order. */
data class BudgetFigures(
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val lines: List<BudgetComponentLine>,
) {
    /** Income minus expenses; negative when the expenses are higher. */
    val monthlyCashFlow: BigDecimal get() = totalIncome.subtract(totalExpenses)

    fun line(component: BudgetComponent): BudgetComponentLine = lines.first { it.component == component }

    /**
     * Share of the income for [component] with 2 decimals, calculated from the amounts (not from the stored
     * 4-decimal value) so that it is never rounded twice.
     */
    fun displayPercentage(component: BudgetComponent): BigDecimal =
        Percentage.of(line(component).amount, totalIncome, Percentage.DISPLAY_SCALE)
}
