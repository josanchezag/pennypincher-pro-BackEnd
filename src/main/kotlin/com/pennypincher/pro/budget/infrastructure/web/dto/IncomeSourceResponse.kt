package com.pennypincher.pro.budget.infrastructure.web.dto

import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Public representation of an income source. */
data class IncomeSourceResponse(
    val id: UUID,
    val name: String,
    val category: IncomeCategory,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Body of `GET /income-sources/total`. */
data class IncomeSourceTotalResponse(val totalMonthlyIncome: BigDecimal)
