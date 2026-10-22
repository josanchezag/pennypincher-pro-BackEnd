package com.pennypincher.pro.budget.infrastructure.web.dto

import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/** Body of `POST /monthly-budgets`; the ranges are checked by the use case. */
data class CreateMonthlyBudgetRequest(
    @field:NotNull(message = RequestMessages.REQUIRED)
    val year: Int? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val month: Int? = null,
    val exchangeRate: BigDecimal? = null,
    val workDays: Int? = null,
)

/** Body of `PUT /monthly-budgets/{id}`: both values are replaced, and an absent one is cleared. */
data class UpdateMonthlyBudgetRequest(val exchangeRate: BigDecimal? = null, val workDays: Int? = null)
