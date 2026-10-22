package com.pennypincher.pro.networth.application.usecase

import com.pennypincher.pro.networth.domain.model.Asset
import com.pennypincher.pro.networth.domain.model.AssetCommand
import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.networth.domain.port.inbound.AssetUseCase
import com.pennypincher.pro.networth.domain.port.outbound.AssetRepository
import com.pennypincher.pro.shared.application.usecase.AbstractCrudUseCase
import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.validation.FieldValidator
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** Asset use cases: the name is trimmed and the value must be a non-negative COP amount. */
class AssetUseCaseImpl(repository: AssetRepository, clock: Clock) :
    AbstractCrudUseCase<Asset, AssetCommand, AssetCategory>(repository, clock),
    AssetUseCase {
    override val resourceName = "Asset"
    override val ordering = Asset.DISPLAY_ORDER

    override fun validate(command: AssetCommand) = FieldValidator()
        .requireText(NAME_FIELD, command.name, Asset.MAX_NAME_LENGTH)
        .requireNonNegativeAmount(VALUE_FIELD, command.value)
        .validate()

    override fun build(userId: UUID, command: AssetCommand, now: Instant) = Asset(
        id = UUID.randomUUID(),
        userId = userId,
        name = command.name.trim(),
        value = Money.of(command.value),
        category = command.category,
        createdAt = now,
        updatedAt = now,
    )

    override fun applyChanges(current: Asset, command: AssetCommand, now: Instant) = current.copy(
        name = command.name.trim(),
        value = Money.of(command.value),
        category = command.category,
        updatedAt = now,
    )

    private companion object {
        const val NAME_FIELD = "name"
        const val VALUE_FIELD = "value"
    }
}
