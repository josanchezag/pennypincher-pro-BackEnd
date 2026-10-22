package com.pennypincher.pro.budget.infrastructure.persistence.adapter

import com.pennypincher.pro.budget.domain.model.BudgetComponentLine
import com.pennypincher.pro.budget.domain.model.BudgetFigures
import com.pennypincher.pro.budget.domain.model.BudgetPeriod
import com.pennypincher.pro.budget.domain.model.BudgetSettings
import com.pennypincher.pro.budget.domain.model.MonthlyBudget
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import com.pennypincher.pro.budget.infrastructure.persistence.entity.BudgetComponentColumns
import com.pennypincher.pro.budget.infrastructure.persistence.entity.MonthlyBudgetEntity

/** Converts between [MonthlyBudget] and [MonthlyBudgetEntity]. */
object MonthlyBudgetEntityMapper {
    fun toEntity(budget: MonthlyBudget): MonthlyBudgetEntity = MonthlyBudgetEntity(
        id = budget.id,
        userId = budget.userId,
        year = budget.period.year,
        month = budget.period.month,
        exchangeRate = budget.settings.exchangeRate,
        workDays = budget.settings.workDays,
        totalIncome = budget.figures.totalIncome,
        totalExpenses = budget.figures.totalExpenses,
        monthlyCashFlow = budget.figures.monthlyCashFlow,
        components = toColumns(budget.figures),
        createdAt = budget.createdAt,
        updatedAt = budget.updatedAt,
    )

    fun toDomain(entity: MonthlyBudgetEntity): MonthlyBudget = MonthlyBudget(
        id = entity.id,
        userId = entity.userId,
        period = BudgetPeriod(entity.year, entity.month),
        settings = BudgetSettings(entity.exchangeRate, entity.workDays),
        figures = BudgetFigures(entity.totalIncome, entity.totalExpenses, toLines(entity.components)),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    private fun toColumns(figures: BudgetFigures): BudgetComponentColumns {
        fun line(component: BudgetComponent) = figures.line(component)
        return BudgetComponentColumns(
            creditCardTotal = line(BudgetComponent.CREDIT_CARDS).amount,
            creditCardPercentage = line(BudgetComponent.CREDIT_CARDS).percentage,
            loanTotal = line(BudgetComponent.LOANS).amount,
            loanPercentage = line(BudgetComponent.LOANS).percentage,
            expenseTotal = line(BudgetComponent.MONTHLY_EXPENSES).amount,
            expensePercentage = line(BudgetComponent.MONTHLY_EXPENSES).percentage,
            savingsTotal = line(BudgetComponent.SAVINGS).amount,
            savingsPercentage = line(BudgetComponent.SAVINGS).percentage,
            charityTotal = line(BudgetComponent.CHARITY).amount,
            charityPercentage = line(BudgetComponent.CHARITY).percentage,
        )
    }

    private fun toLines(columns: BudgetComponentColumns): List<BudgetComponentLine> = listOf(
        BudgetComponentLine(BudgetComponent.CREDIT_CARDS, columns.creditCardTotal, columns.creditCardPercentage),
        BudgetComponentLine(BudgetComponent.LOANS, columns.loanTotal, columns.loanPercentage),
        BudgetComponentLine(BudgetComponent.MONTHLY_EXPENSES, columns.expenseTotal, columns.expensePercentage),
        BudgetComponentLine(BudgetComponent.SAVINGS, columns.savingsTotal, columns.savingsPercentage),
        BudgetComponentLine(BudgetComponent.CHARITY, columns.charityTotal, columns.charityPercentage),
    )
}
