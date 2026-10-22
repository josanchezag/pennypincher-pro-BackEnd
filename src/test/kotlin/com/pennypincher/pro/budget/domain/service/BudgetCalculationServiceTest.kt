package com.pennypincher.pro.budget.domain.service

import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BudgetCalculationServiceTest {
    private val service = BudgetCalculationService()

    @Test
    fun should_match_the_spreadsheet_figures_when_income_covers_loans_and_expenses() {
        val figures = service.calculate(
            BigDecimal("37152922.36"),
            mapOf(
                BudgetComponent.LOANS to BigDecimal("3280231.00"),
                BudgetComponent.MONTHLY_EXPENSES to BigDecimal("15854970.17"),
            ),
        )

        assertEquals("37152922.36", figures.totalIncome.toPlainString())
        assertEquals("19135201.17", figures.totalExpenses.toPlainString())
        assertEquals("18017721.19", figures.monthlyCashFlow.toPlainString())
        assertEquals("8.8290", figures.line(BudgetComponent.LOANS).percentage.toPlainString())
        assertEquals("42.6749", figures.line(BudgetComponent.MONTHLY_EXPENSES).percentage.toPlainString())
        assertEquals("8.83", figures.displayPercentage(BudgetComponent.LOANS).toPlainString())
        assertEquals("42.67", figures.displayPercentage(BudgetComponent.MONTHLY_EXPENSES).toPlainString())
        assertEquals("0.00", figures.line(BudgetComponent.CREDIT_CARDS).amount.toPlainString())
        assertEquals("0.0000", figures.line(BudgetComponent.CREDIT_CARDS).percentage.toPlainString())
    }

    @Test
    fun should_list_every_component_in_declaration_order_when_some_amounts_are_missing() {
        val figures = service.calculate(BigDecimal("100.00"), emptyMap())

        assertEquals(BudgetComponent.entries, figures.lines.map { it.component })
        assertEquals("0.00", figures.totalExpenses.toPlainString())
        assertEquals("100.00", figures.monthlyCashFlow.toPlainString())
    }

    @Test
    fun should_return_zero_percentages_when_there_is_no_income() {
        val figures = service.calculate(BigDecimal.ZERO, mapOf(BudgetComponent.LOANS to BigDecimal("500.00")))

        assertEquals("0.00", figures.totalIncome.toPlainString())
        assertEquals("-500.00", figures.monthlyCashFlow.toPlainString())
        figures.lines.forEach { assertEquals(0, it.percentage.signum()) }
        assertEquals("0.00", figures.displayPercentage(BudgetComponent.LOANS).toPlainString())
    }

    @Test
    fun should_return_negative_cash_flow_and_shares_above_one_hundred_when_expenses_exceed_income() {
        val figures = service.calculate(
            BigDecimal("1000.00"),
            mapOf(BudgetComponent.CREDIT_CARDS to BigDecimal("900.00"), BudgetComponent.LOANS to BigDecimal("600.00")),
        )

        assertEquals("1500.00", figures.totalExpenses.toPlainString())
        assertEquals("-500.00", figures.monthlyCashFlow.toPlainString())
        assertEquals("90.00", figures.displayPercentage(BudgetComponent.CREDIT_CARDS).toPlainString())
        assertEquals("60.00", figures.displayPercentage(BudgetComponent.LOANS).toPlainString())
    }

    @Test
    fun should_count_charity_but_not_savings_as_expenses() {
        val figures = service.calculate(
            BigDecimal("1000.00"),
            mapOf(
                BudgetComponent.MONTHLY_EXPENSES to BigDecimal("300.00"),
                BudgetComponent.SAVINGS to BigDecimal("100.00"),
                BudgetComponent.CHARITY to BigDecimal("50.00"),
            ),
        )

        assertEquals("350.00", figures.totalExpenses.toPlainString())
        assertEquals("650.00", figures.monthlyCashFlow.toPlainString())
        assertEquals("10.00", figures.displayPercentage(BudgetComponent.SAVINGS).toPlainString())
        assertEquals("5.00", figures.displayPercentage(BudgetComponent.CHARITY).toPlainString())
    }

    @Test
    fun should_round_half_up_without_rounding_twice_when_showing_two_decimals() {
        val figures = service.calculate(
            BigDecimal("1000000.00"),
            mapOf(
                BudgetComponent.LOANS to BigDecimal("88349.51"),
                BudgetComponent.CHARITY to BigDecimal("125.00"),
            ),
        )

        assertEquals("8.8350", figures.line(BudgetComponent.LOANS).percentage.toPlainString())
        assertEquals("8.83", figures.displayPercentage(BudgetComponent.LOANS).toPlainString())
        assertEquals("0.0125", figures.line(BudgetComponent.CHARITY).percentage.toPlainString())
        assertEquals("0.01", figures.displayPercentage(BudgetComponent.CHARITY).toPlainString())
    }

    @Test
    fun should_cap_share_so_it_fits_storage_when_amount_dwarfs_income() {
        val figures = service.calculate(
            BigDecimal("0.01"),
            mapOf(BudgetComponent.SAVINGS to BigDecimal("9999999999999999.99")),
        )

        assertEquals("99999.9900", figures.line(BudgetComponent.SAVINGS).percentage.toPlainString())
        assertEquals("99999.99", figures.displayPercentage(BudgetComponent.SAVINGS).toPlainString())
    }
}
