package com.pennypincher.pro.expenses.infrastructure.web.mapper

import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.expenses.domain.model.ExpenseCommand
import com.pennypincher.pro.expenses.domain.model.ExpenseSummary
import com.pennypincher.pro.expenses.infrastructure.web.dto.CategoryTotalResponse
import com.pennypincher.pro.expenses.infrastructure.web.dto.ExpenseRequest
import com.pennypincher.pro.expenses.infrastructure.web.dto.ExpenseResponse
import com.pennypincher.pro.expenses.infrastructure.web.dto.ExpenseSummaryResponse

/** Converts between the expense HTTP DTOs and the domain, for every kind of expense. */
object ExpenseWebMapper {
    /** Expects a request that already passed bean validation, so no field is null. */
    fun <C : Enum<C>> toCommand(request: ExpenseRequest<C>): ExpenseCommand<C> = ExpenseCommand(
        name = checkNotNull(request.name),
        monthlyAmount = checkNotNull(request.monthlyAmount),
        category = checkNotNull(request.category),
    )

    fun <C : Enum<C>> toResponse(expense: Expense<C>): ExpenseResponse<C> = ExpenseResponse(
        id = expense.id,
        name = expense.name,
        monthlyAmount = expense.monthlyAmount,
        category = expense.category,
        createdAt = expense.createdAt,
        updatedAt = expense.updatedAt,
    )

    fun <C : Enum<C>> toSummaryResponse(summary: ExpenseSummary<C>): ExpenseSummaryResponse<C> = ExpenseSummaryResponse(
        totalMonthlyAmount = summary.totalMonthlyAmount,
        count = summary.count,
        byCategory = summary.byCategory.map { CategoryTotalResponse(it.category, it.totalMonthlyAmount, it.count) },
    )
}
