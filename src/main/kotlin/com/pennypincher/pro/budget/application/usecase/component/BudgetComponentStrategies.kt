package com.pennypincher.pro.budget.application.usecase.component

import com.pennypincher.pro.budget.domain.model.BudgetSourceAmounts
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import com.pennypincher.pro.budget.domain.service.BudgetComponentStrategy
import java.math.BigDecimal

/** Credit cards: the chosen monthly payment of every active card. */
class CreditCardComponentStrategy : BudgetComponentStrategy {
    override val component = BudgetComponent.CREDIT_CARDS

    override fun monthlyAmount(sources: BudgetSourceAmounts): BigDecimal = sources.creditCardPayments
}

/** Loans: the monthly installment of every active loan. */
class LoanComponentStrategy : BudgetComponentStrategy {
    override val component = BudgetComponent.LOANS

    override fun monthlyAmount(sources: BudgetSourceAmounts): BigDecimal = sources.loanPayments
}

/** Monthly expenses: every fixed expense except charity, plus every variable expense. */
class MonthlyExpenseComponentStrategy : BudgetComponentStrategy {
    override val component = BudgetComponent.MONTHLY_EXPENSES

    override fun monthlyAmount(sources: BudgetSourceAmounts): BigDecimal =
        sources.nonCharityFixedExpenses.add(sources.variableExpenses)
}

/** Savings: the monthly contribution to every active savings account. */
class SavingsComponentStrategy : BudgetComponentStrategy {
    override val component = BudgetComponent.SAVINGS

    override fun monthlyAmount(sources: BudgetSourceAmounts): BigDecimal = sources.savingsContributions
}

/** Charity: the fixed expenses of the charity category. */
class CharityComponentStrategy : BudgetComponentStrategy {
    override val component = BudgetComponent.CHARITY

    override fun monthlyAmount(sources: BudgetSourceAmounts): BigDecimal = sources.charityFixedExpenses
}

/** One strategy per [BudgetComponent]. */
object BudgetComponentStrategies {
    fun all(): List<BudgetComponentStrategy> = listOf(
        CreditCardComponentStrategy(),
        LoanComponentStrategy(),
        MonthlyExpenseComponentStrategy(),
        SavingsComponentStrategy(),
        CharityComponentStrategy(),
    )
}
