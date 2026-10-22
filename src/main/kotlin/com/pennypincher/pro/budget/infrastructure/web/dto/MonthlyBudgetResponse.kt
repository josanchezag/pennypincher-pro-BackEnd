package com.pennypincher.pro.budget.infrastructure.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** One row of the budget breakdown; `idealPercentage` is null when the component has no target. */
data class BudgetComponentResponse(
    val component: BudgetComponent,
    val amount: BigDecimal,
    val percentage: BigDecimal,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val idealPercentage: BigDecimal?,
)

/** Public representation of a monthly budget; percentages are in percent units with 2 decimals. */
data class MonthlyBudgetResponse(
    val id: UUID,
    val year: Int,
    val month: Int,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val exchangeRate: BigDecimal?,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val workDays: Int?,
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val monthlyCashFlow: BigDecimal,
    val creditCardPercentage: BigDecimal,
    val loanPercentage: BigDecimal,
    val expensePercentage: BigDecimal,
    val savingsPercentage: BigDecimal,
    val charityPercentage: BigDecimal,
    val components: List<BudgetComponentResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
