package com.pennypincher.pro.budget.infrastructure.web.mapper

import com.pennypincher.pro.budget.domain.model.IncomeSource
import com.pennypincher.pro.budget.domain.model.IncomeSourceCommand
import com.pennypincher.pro.budget.infrastructure.web.dto.IncomeSourceRequest
import com.pennypincher.pro.budget.infrastructure.web.dto.IncomeSourceResponse

/** Converts between the income source HTTP DTOs and the domain. */
object IncomeSourceWebMapper {
    /** Expects a request that already passed bean validation, so no required field is null. */
    fun toCommand(request: IncomeSourceRequest): IncomeSourceCommand = IncomeSourceCommand(
        name = checkNotNull(request.name),
        category = checkNotNull(request.category),
        amount = checkNotNull(request.amount),
        currency = request.currency,
    )

    fun toResponse(source: IncomeSource): IncomeSourceResponse = IncomeSourceResponse(
        id = source.id,
        name = source.name,
        category = source.category,
        amount = source.amount,
        currency = source.currency,
        createdAt = source.createdAt,
        updatedAt = source.updatedAt,
    )
}
