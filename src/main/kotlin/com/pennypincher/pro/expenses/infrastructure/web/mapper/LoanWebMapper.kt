package com.pennypincher.pro.expenses.infrastructure.web.mapper

import com.pennypincher.pro.expenses.domain.model.Loan
import com.pennypincher.pro.expenses.domain.model.LoanCommand
import com.pennypincher.pro.expenses.infrastructure.web.dto.LoanRequest
import com.pennypincher.pro.expenses.infrastructure.web.dto.LoanResponse

/** Converts between the loan HTTP DTOs and the domain. */
object LoanWebMapper {
    /** Expects a request that already passed bean validation, so no required field is null. */
    fun toCommand(request: LoanRequest): LoanCommand = LoanCommand(
        name = checkNotNull(request.name),
        monthlyPayment = checkNotNull(request.monthlyPayment),
        totalBalance = checkNotNull(request.totalBalance),
        originalAmount = request.originalAmount,
    )

    fun toResponse(loan: Loan): LoanResponse = LoanResponse(
        id = loan.id,
        name = loan.name,
        monthlyPayment = loan.monthlyPayment,
        totalBalance = loan.totalBalance,
        originalAmount = loan.originalAmount,
        createdAt = loan.createdAt,
        updatedAt = loan.updatedAt,
    )
}
