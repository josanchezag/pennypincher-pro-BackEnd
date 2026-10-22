package com.pennypincher.pro.budget.application.usecase.component

import com.pennypincher.pro.budget.domain.model.BudgetSourceAmounts
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BudgetComponentStrategiesTest {
    private val sources = BudgetSourceAmounts(
        totalIncome = BigDecimal("100.00"),
        creditCardPayments = BigDecimal("1.00"),
        loanPayments = BigDecimal("2.00"),
        nonCharityFixedExpenses = BigDecimal("3.00"),
        charityFixedExpenses = BigDecimal("6.00"),
        variableExpenses = BigDecimal("4.00"),
        savingsContributions = BigDecimal("5.00"),
    )

    @Test
    fun should_provide_one_strategy_per_component_in_declaration_order() {
        assertEquals(BudgetComponent.entries, BudgetComponentStrategies.all().map { it.component })
    }

    @Test
    fun should_read_each_component_from_its_source_and_split_charity_from_monthly_expenses() {
        val amounts = BudgetComponentStrategies.all().associate { it.component to it.monthlyAmount(sources) }

        assertEquals(
            mapOf(
                BudgetComponent.CREDIT_CARDS to BigDecimal("1.00"),
                BudgetComponent.LOANS to BigDecimal("2.00"),
                BudgetComponent.MONTHLY_EXPENSES to BigDecimal("7.00"),
                BudgetComponent.SAVINGS to BigDecimal("5.00"),
                BudgetComponent.CHARITY to BigDecimal("6.00"),
            ),
            amounts,
        )
    }
}
