package com.pennypincher.pro.budget.domain.service

import com.pennypincher.pro.budget.domain.model.BudgetComponentLine
import com.pennypincher.pro.budget.domain.model.BudgetFigures
import com.pennypincher.pro.budget.domain.model.Percentage
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import com.pennypincher.pro.shared.domain.model.Money
import java.math.BigDecimal

/**
 * Pure budget arithmetic. Total expenses are the components that [count as expenses][BudgetComponent.countsAsExpense]
 * (savings do not); each component's share is relative to the total income and is zero when there is no income.
 */
class BudgetCalculationService {
    /** Figures for [totalIncome] and the monthly [amounts] per component; a missing component counts as zero. */
    fun calculate(totalIncome: BigDecimal, amounts: Map<BudgetComponent, BigDecimal>): BudgetFigures {
        val lines = BudgetComponent.entries.map { component ->
            val amount = Money.of(amounts[component] ?: BigDecimal.ZERO)
            BudgetComponentLine(component, amount, Percentage.of(amount, totalIncome, Percentage.STORED_SCALE))
        }
        val totalExpenses = lines.filter { it.component.countsAsExpense }.sumOf { it.amount }
        return BudgetFigures(Money.of(totalIncome), Money.of(totalExpenses), lines)
    }
}
