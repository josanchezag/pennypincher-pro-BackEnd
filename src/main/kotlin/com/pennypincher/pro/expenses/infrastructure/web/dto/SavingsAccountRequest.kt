package com.pennypincher.pro.expenses.infrastructure.web.dto

import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/**
 * Body of `POST /savings-accounts` and `PUT /savings-accounts/{id}`; the business rules are checked by the
 * use case.
 */
data class SavingsAccountRequest(
    @field:NotNull(message = RequestMessages.REQUIRED)
    val name: String? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val monthlyContribution: BigDecimal? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val currentBalance: BigDecimal? = null,
)
