package com.pennypincher.pro.expenses.domain.model

import com.pennypincher.pro.shared.domain.model.Money
import java.math.BigDecimal

/** Total monthly amount and number of the active expenses of one category. */
data class CategoryTotal<C : Enum<C>>(val category: C, val totalMonthlyAmount: BigDecimal, val count: Long)

/**
 * Totals of a user's active expenses, overall and per category. [byCategory] lists only the categories
 * that have active expenses, in the enum's declaration order.
 */
data class ExpenseSummary<C : Enum<C>>(
    val totalMonthlyAmount: BigDecimal,
    val count: Long,
    val byCategory: List<CategoryTotal<C>>,
) {
    companion object {
        /** Builds the summary from per-category totals given in any order; amounts get two decimal places. */
        fun <C : Enum<C>> of(categoryTotals: List<CategoryTotal<C>>): ExpenseSummary<C> {
            val byCategory = categoryTotals
                .sortedBy { it.category }
                .map { it.copy(totalMonthlyAmount = Money.of(it.totalMonthlyAmount)) }
            return ExpenseSummary(
                totalMonthlyAmount = Money.of(byCategory.sumOf { it.totalMonthlyAmount }),
                count = byCategory.sumOf { it.count },
                byCategory = byCategory,
            )
        }
    }
}
