package com.pennypincher.pro.expenses.infrastructure.web.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Public representation of a fixed or variable expense. */
data class ExpenseResponse<C : Enum<C>>(
    val id: UUID,
    val name: String,
    val monthlyAmount: BigDecimal,
    val category: C,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Body of `GET /fixed-expenses/summary` and `GET /variable-expenses/summary`. */
data class ExpenseSummaryResponse<C : Enum<C>>(
    val totalMonthlyAmount: BigDecimal,
    val count: Long,
    val byCategory: List<CategoryTotalResponse<C>>,
)

/** One category of an [ExpenseSummaryResponse]. */
data class CategoryTotalResponse<C : Enum<C>>(val category: C, val totalMonthlyAmount: BigDecimal, val count: Long)
