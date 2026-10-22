package com.pennypincher.pro.expenses.domain.model

import com.pennypincher.pro.expenses.domain.model.enums.FixedExpenseCategory
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExpenseSummaryTest {
    @Test
    fun should_order_categories_by_declaration_and_add_them_up_when_building() {
        val summary = ExpenseSummary.of(
            listOf(
                CategoryTotal(FixedExpenseCategory.CHARITY, BigDecimal("50000"), 1),
                CategoryTotal(FixedExpenseCategory.FOOD, BigDecimal("1100000.5"), 1),
                CategoryTotal(FixedExpenseCategory.EDUCATION, BigDecimal("2347709"), 2),
            ),
        )

        assertEquals(
            listOf(FixedExpenseCategory.FOOD, FixedExpenseCategory.EDUCATION, FixedExpenseCategory.CHARITY),
            summary.byCategory.map { it.category },
        )
        assertEquals(
            listOf("1100000.50", "2347709.00", "50000.00"),
            summary.byCategory.map {
                it.totalMonthlyAmount.toPlainString()
            },
        )
        assertEquals("3497709.50", summary.totalMonthlyAmount.toPlainString())
        assertEquals(4, summary.count)
    }

    @Test
    fun should_return_zero_totals_and_no_categories_when_there_are_no_expenses() {
        val summary = ExpenseSummary.of(emptyList<CategoryTotal<FixedExpenseCategory>>())

        assertEquals("0.00", summary.totalMonthlyAmount.toPlainString())
        assertEquals(0, summary.count)
        assertEquals(emptyList<CategoryTotal<FixedExpenseCategory>>(), summary.byCategory)
    }
}
