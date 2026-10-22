package com.pennypincher.pro.expenses.infrastructure.web.mapper

import com.pennypincher.pro.expenses.domain.model.CreditCard
import com.pennypincher.pro.expenses.domain.model.CreditCardCommand
import com.pennypincher.pro.expenses.infrastructure.web.dto.CreditCardRequest
import com.pennypincher.pro.expenses.infrastructure.web.dto.CreditCardResponse

/** Converts between the credit card HTTP DTOs and the domain. */
object CreditCardWebMapper {
    /** Expects a request that already passed bean validation, so no field is null. */
    fun toCommand(request: CreditCardRequest): CreditCardCommand = CreditCardCommand(
        name = checkNotNull(request.name),
        monthlyPayment = checkNotNull(request.monthlyPayment),
        totalBalance = checkNotNull(request.totalBalance),
    )

    fun toResponse(card: CreditCard): CreditCardResponse = CreditCardResponse(
        id = card.id,
        name = card.name,
        monthlyPayment = card.monthlyPayment,
        totalBalance = card.totalBalance,
        createdAt = card.createdAt,
        updatedAt = card.updatedAt,
    )
}
