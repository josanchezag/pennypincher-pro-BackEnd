package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.SavingsAccount
import com.pennypincher.pro.expenses.domain.model.SavingsAccountCommand
import com.pennypincher.pro.expenses.domain.port.inbound.SavingsAccountUseCase
import com.pennypincher.pro.expenses.domain.port.outbound.SavingsAccountRepository
import com.pennypincher.pro.shared.domain.model.Money
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** Savings account use cases: both amounts are required; the shared rules come from the base class. */
class SavingsAccountUseCaseImpl(repository: SavingsAccountRepository, clock: Clock) :
    AbstractBalanceAccountUseCase<SavingsAccount, SavingsAccountCommand>(repository, clock),
    SavingsAccountUseCase {
    override val resourceName = "Savings account"

    override fun amountsOf(command: SavingsAccountCommand) = mapOf(
        MONTHLY_CONTRIBUTION_FIELD to command.monthlyContribution,
        CURRENT_BALANCE_FIELD to command.currentBalance,
    )

    override fun build(userId: UUID, command: SavingsAccountCommand, now: Instant) = SavingsAccount(
        id = UUID.randomUUID(),
        userId = userId,
        name = command.name.trim(),
        monthlyContribution = Money.of(command.monthlyContribution),
        currentBalance = Money.of(command.currentBalance),
        createdAt = now,
        updatedAt = now,
    )

    override fun applyChanges(current: SavingsAccount, command: SavingsAccountCommand, now: Instant) = current.copy(
        name = command.name.trim(),
        monthlyContribution = Money.of(command.monthlyContribution),
        currentBalance = Money.of(command.currentBalance),
        updatedAt = now,
    )

    private companion object {
        const val MONTHLY_CONTRIBUTION_FIELD = "monthlyContribution"
        const val CURRENT_BALANCE_FIELD = "currentBalance"
    }
}
