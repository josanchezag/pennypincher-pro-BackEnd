package com.pennypincher.pro.budget.infrastructure.web.mapper

import com.pennypincher.pro.budget.domain.model.BudgetFigures
import com.pennypincher.pro.budget.domain.model.BudgetSettings
import com.pennypincher.pro.budget.domain.model.CreateBudgetCommand
import com.pennypincher.pro.budget.domain.model.MonthlyBudget
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import com.pennypincher.pro.budget.infrastructure.web.dto.BudgetComponentResponse
import com.pennypincher.pro.budget.infrastructure.web.dto.CreateMonthlyBudgetRequest
import com.pennypincher.pro.budget.infrastructure.web.dto.MonthlyBudgetResponse
import com.pennypincher.pro.budget.infrastructure.web.dto.UpdateMonthlyBudgetRequest

/** Converts between the monthly budget HTTP DTOs and the domain. */
object MonthlyBudgetWebMapper {
    /** Expects a request that already passed bean validation, so `year` and `month` are not null. */
    fun toCommand(request: CreateMonthlyBudgetRequest): CreateBudgetCommand = CreateBudgetCommand(
        year = checkNotNull(request.year),
        month = checkNotNull(request.month),
        settings = BudgetSettings(request.exchangeRate, request.workDays),
    )

    fun toSettings(request: UpdateMonthlyBudgetRequest): BudgetSettings =
        BudgetSettings(request.exchangeRate, request.workDays)

    fun toResponse(budget: MonthlyBudget): MonthlyBudgetResponse {
        val figures = budget.figures
        return MonthlyBudgetResponse(
            id = budget.id,
            year = budget.period.year,
            month = budget.period.month,
            exchangeRate = budget.settings.exchangeRate,
            workDays = budget.settings.workDays,
            totalIncome = figures.totalIncome,
            totalExpenses = figures.totalExpenses,
            monthlyCashFlow = figures.monthlyCashFlow,
            creditCardPercentage = figures.displayPercentage(BudgetComponent.CREDIT_CARDS),
            loanPercentage = figures.displayPercentage(BudgetComponent.LOANS),
            expensePercentage = figures.displayPercentage(BudgetComponent.MONTHLY_EXPENSES),
            savingsPercentage = figures.displayPercentage(BudgetComponent.SAVINGS),
            charityPercentage = figures.displayPercentage(BudgetComponent.CHARITY),
            components = BudgetComponent.entries.map { componentOf(figures, it) },
            createdAt = budget.createdAt,
            updatedAt = budget.updatedAt,
        )
    }

    private fun componentOf(figures: BudgetFigures, component: BudgetComponent) = BudgetComponentResponse(
        component = component,
        amount = figures.line(component).amount,
        percentage = figures.displayPercentage(component),
        idealPercentage = component.idealPercentage,
    )
}
