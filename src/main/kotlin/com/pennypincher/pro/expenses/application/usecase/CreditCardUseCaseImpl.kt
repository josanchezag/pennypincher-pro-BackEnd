package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.CreditCard
import com.pennypincher.pro.expenses.domain.model.CreditCardCommand
import com.pennypincher.pro.expenses.domain.port.inbound.CreditCardUseCase
import com.pennypincher.pro.expenses.domain.port.outbound.CreditCardRepository
import com.pennypincher.pro.shared.domain.model.Money
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** Credit card use cases: both amounts are required; the shared rules come from the base class. */
class CreditCardUseCaseImpl(repository: CreditCardRepository, clock: Clock) :
    AbstractBalanceAccountUseCase<CreditCard, CreditCardCommand>(repository, clock),
    CreditCardUseCase {
    override val resourceName = "Credit card"

    override fun amountsOf(command: CreditCardCommand) = mapOf(
        DebtFields.MONTHLY_PAYMENT to command.monthlyPayment,
        DebtFields.TOTAL_BALANCE to command.totalBalance,
    )

    override fun build(userId: UUID, command: CreditCardCommand, now: Instant) = CreditCard(
        id = UUID.randomUUID(),
        userId = userId,
        name = command.name.trim(),
        monthlyPayment = Money.of(command.monthlyPayment),
        totalBalance = Money.of(command.totalBalance),
        createdAt = now,
        updatedAt = now,
    )

    override fun applyChanges(current: CreditCard, command: CreditCardCommand, now: Instant) = current.copy(
        name = command.name.trim(),
        monthlyPayment = Money.of(command.monthlyPayment),
        totalBalance = Money.of(command.totalBalance),
        updatedAt = now,
    )
}
