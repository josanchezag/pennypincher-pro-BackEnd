package com.pennypincher.pro.budget.infrastructure.web.dto

import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/**
 * Body of `POST /income-sources` and `PUT /income-sources/{id}`; `currency` is optional (COP when absent).
 * The business rules are checked by the use case.
 */
data class IncomeSourceRequest(
    @field:NotNull(message = RequestMessages.REQUIRED)
    val name: String? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val category: IncomeCategory? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val amount: BigDecimal? = null,
    val currency: String? = null,
)
