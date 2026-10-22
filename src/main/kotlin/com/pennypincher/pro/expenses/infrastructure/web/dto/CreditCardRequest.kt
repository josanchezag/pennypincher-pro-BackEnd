package com.pennypincher.pro.expenses.infrastructure.web.dto

import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/** Body of `POST /credit-cards` and `PUT /credit-cards/{id}`; the business rules are checked by the use case. */
data class CreditCardRequest(
    @field:NotNull(message = RequestMessages.REQUIRED)
    val name: String? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val monthlyPayment: BigDecimal? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val totalBalance: BigDecimal? = null,
)
