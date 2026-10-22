package com.pennypincher.pro.expenses.infrastructure.web.mapper

import com.pennypincher.pro.expenses.domain.model.SavingsAccount
import com.pennypincher.pro.expenses.domain.model.SavingsAccountCommand
import com.pennypincher.pro.expenses.infrastructure.web.dto.SavingsAccountRequest
import com.pennypincher.pro.expenses.infrastructure.web.dto.SavingsAccountResponse

/** Converts between the savings account HTTP DTOs and the domain. */
object SavingsAccountWebMapper {
    /** Expects a request that already passed bean validation, so no field is null. */
    fun toCommand(request: SavingsAccountRequest): SavingsAccountCommand = SavingsAccountCommand(
        name = checkNotNull(request.name),
        monthlyContribution = checkNotNull(request.monthlyContribution),
        currentBalance = checkNotNull(request.currentBalance),
    )

    fun toResponse(account: SavingsAccount): SavingsAccountResponse = SavingsAccountResponse(
        id = account.id,
        name = account.name,
        monthlyContribution = account.monthlyContribution,
        currentBalance = account.currentBalance,
        createdAt = account.createdAt,
        updatedAt = account.updatedAt,
    )
}
