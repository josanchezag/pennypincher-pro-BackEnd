package com.pennypincher.pro.shared.domain.port.outbound

import java.math.BigDecimal
import java.util.UUID

/** A [UserScopedCrudRepository] of records with a single amount that can be added up. */
interface SummableCrudRepository<T, C> : UserScopedCrudRepository<T, C> {
    /** Sum of the amounts of the active records of the user (zero when there are none). */
    fun sumActive(userId: UUID): BigDecimal
}
