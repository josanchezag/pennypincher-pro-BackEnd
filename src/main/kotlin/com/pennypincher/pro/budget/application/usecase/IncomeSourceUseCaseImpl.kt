package com.pennypincher.pro.budget.application.usecase

import com.pennypincher.pro.budget.domain.model.IncomeSource
import com.pennypincher.pro.budget.domain.model.IncomeSourceCommand
import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import com.pennypincher.pro.budget.domain.port.inbound.IncomeSourceUseCase
import com.pennypincher.pro.budget.domain.port.outbound.IncomeSourceRepository
import com.pennypincher.pro.shared.application.usecase.AbstractCrudUseCase
import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.validation.FieldValidator
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Income source use cases: the name is trimmed, the amount must be a non-negative COP amount and the
 * currency, when given, an ISO 4217 code (it defaults to COP, also on update).
 */
class IncomeSourceUseCaseImpl(repository: IncomeSourceRepository, clock: Clock) :
    AbstractCrudUseCase<IncomeSource, IncomeSourceCommand, IncomeCategory>(repository, clock),
    IncomeSourceUseCase {
    override val resourceName = "Income source"
    override val ordering = IncomeSource.DISPLAY_ORDER

    override fun validate(command: IncomeSourceCommand) = FieldValidator()
        .requireText(NAME_FIELD, command.name, IncomeSource.MAX_NAME_LENGTH)
        .requireNonNegativeAmount(AMOUNT_FIELD, command.amount)
        .require(command.currency?.matches(IncomeSource.CURRENCY_FORMAT) ?: true, CURRENCY_FIELD, CURRENCY_MESSAGE)
        .validate()

    override fun build(userId: UUID, command: IncomeSourceCommand, now: Instant) = IncomeSource(
        id = UUID.randomUUID(),
        userId = userId,
        name = command.name.trim(),
        category = command.category,
        amount = Money.of(command.amount),
        currency = currencyOf(command),
        createdAt = now,
        updatedAt = now,
    )

    override fun applyChanges(current: IncomeSource, command: IncomeSourceCommand, now: Instant) = current.copy(
        name = command.name.trim(),
        category = command.category,
        amount = Money.of(command.amount),
        currency = currencyOf(command),
        updatedAt = now,
    )

    private fun currencyOf(command: IncomeSourceCommand): String = command.currency ?: IncomeSource.DEFAULT_CURRENCY

    private companion object {
        const val NAME_FIELD = "name"
        const val AMOUNT_FIELD = "amount"
        const val CURRENCY_FIELD = "currency"
        const val CURRENCY_MESSAGE = "must be a three-letter upper-case ISO 4217 code"
    }
}
