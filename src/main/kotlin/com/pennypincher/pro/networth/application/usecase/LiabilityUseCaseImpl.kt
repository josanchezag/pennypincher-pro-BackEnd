package com.pennypincher.pro.networth.application.usecase

import com.pennypincher.pro.networth.domain.model.Liability
import com.pennypincher.pro.networth.domain.model.LiabilityCommand
import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import com.pennypincher.pro.networth.domain.port.inbound.LiabilityUseCase
import com.pennypincher.pro.networth.domain.port.outbound.LiabilityRepository
import com.pennypincher.pro.shared.application.usecase.AbstractCrudUseCase
import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.validation.FieldValidator
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** Liability use cases: the name is trimmed and the current balance must be a non-negative COP amount. */
class LiabilityUseCaseImpl(repository: LiabilityRepository, clock: Clock) :
    AbstractCrudUseCase<Liability, LiabilityCommand, LiabilityCategory>(repository, clock),
    LiabilityUseCase {
    override val resourceName = "Liability"
    override val ordering = Liability.DISPLAY_ORDER

    override fun validate(command: LiabilityCommand) = FieldValidator()
        .requireText(NAME_FIELD, command.name, Liability.MAX_NAME_LENGTH)
        .requireNonNegativeAmount(CURRENT_BALANCE_FIELD, command.currentBalance)
        .validate()

    override fun build(userId: UUID, command: LiabilityCommand, now: Instant) = Liability(
        id = UUID.randomUUID(),
        userId = userId,
        name = command.name.trim(),
        currentBalance = Money.of(command.currentBalance),
        category = command.category,
        createdAt = now,
        updatedAt = now,
    )

    override fun applyChanges(current: Liability, command: LiabilityCommand, now: Instant) = current.copy(
        name = command.name.trim(),
        currentBalance = Money.of(command.currentBalance),
        category = command.category,
        updatedAt = now,
    )

    private companion object {
        const val NAME_FIELD = "name"
        const val CURRENT_BALANCE_FIELD = "currentBalance"
    }
}
