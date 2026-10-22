package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.Loan
import com.pennypincher.pro.expenses.domain.model.LoanCommand
import com.pennypincher.pro.expenses.domain.port.inbound.LoanUseCase
import com.pennypincher.pro.expenses.domain.port.outbound.LoanRepository
import com.pennypincher.pro.shared.domain.model.Money
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Loan use cases: the installment and balance are required, the original amount is optional and an
 * update without it clears it. The shared rules come from the base class.
 */
class LoanUseCaseImpl(repository: LoanRepository, clock: Clock) :
    AbstractBalanceAccountUseCase<Loan, LoanCommand>(repository, clock),
    LoanUseCase {
    override val resourceName = "Loan"

    override fun amountsOf(command: LoanCommand) = mapOf(
        DebtFields.MONTHLY_PAYMENT to command.monthlyPayment,
        DebtFields.TOTAL_BALANCE to command.totalBalance,
        ORIGINAL_AMOUNT_FIELD to command.originalAmount,
    )

    override fun build(userId: UUID, command: LoanCommand, now: Instant) = Loan(
        id = UUID.randomUUID(),
        userId = userId,
        name = command.name.trim(),
        monthlyPayment = Money.of(command.monthlyPayment),
        totalBalance = Money.of(command.totalBalance),
        originalAmount = command.originalAmount?.let(Money::of),
        createdAt = now,
        updatedAt = now,
    )

    override fun applyChanges(current: Loan, command: LoanCommand, now: Instant) = current.copy(
        name = command.name.trim(),
        monthlyPayment = Money.of(command.monthlyPayment),
        totalBalance = Money.of(command.totalBalance),
        originalAmount = command.originalAmount?.let(Money::of),
        updatedAt = now,
    )

    private companion object {
        const val ORIGINAL_AMOUNT_FIELD = "originalAmount"
    }
}
