package com.pennypincher.pro.expenses.infrastructure.web.dto

import com.pennypincher.pro.expenses.domain.model.enums.FixedExpenseCategory
import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/**
 * Body of `POST` and `PUT` on the expense endpoints; the business rules are checked by the use case. Each
 * kind of expense has a concrete subclass so that JSON knows which category enum to read.
 */
open class ExpenseRequest<C : Enum<C>> {
    @field:NotNull(message = RequestMessages.REQUIRED)
    var name: String? = null

    @field:NotNull(message = RequestMessages.REQUIRED)
    var monthlyAmount: BigDecimal? = null

    @field:NotNull(message = RequestMessages.REQUIRED)
    var category: C? = null
}

/** Body of `POST /fixed-expenses` and `PUT /fixed-expenses/{id}`. */
class FixedExpenseRequest : ExpenseRequest<FixedExpenseCategory>()
