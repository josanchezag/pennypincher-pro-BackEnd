package com.pennypincher.pro.shared.application.usecase

import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.model.OwnedResource
import com.pennypincher.pro.shared.domain.port.inbound.TotalUseCase
import com.pennypincher.pro.shared.domain.port.outbound.SummableCrudRepository
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID

/** [AbstractCrudUseCase] for records with a single amount, which also answers their active [total]. */
abstract class AbstractTotalCrudUseCase<T : OwnedResource, Cmd, C>(
    private val repository: SummableCrudRepository<T, C>,
    clock: Clock,
) : AbstractCrudUseCase<T, Cmd, C>(repository, clock),
    TotalUseCase {
    final override fun total(userId: UUID): BigDecimal = Money.of(repository.sumActive(userId))
}
