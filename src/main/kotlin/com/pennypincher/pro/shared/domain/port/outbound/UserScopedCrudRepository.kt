package com.pennypincher.pro.shared.domain.port.outbound

import java.math.BigDecimal
import java.util.UUID

/**
 * Persistence port shared by the simple per-user records (assets, liabilities, ...). Every method is
 * scoped by `userId`, and only active (not soft-deleted) rows are visible.
 */
interface UserScopedCrudRepository<T, C> {
    /** Active records of the user, optionally restricted to one [category]; the order is not defined. */
    fun findAllActive(userId: UUID, category: C?): List<T>

    fun findActiveById(userId: UUID, id: UUID): T?

    /** Inserts or updates the record; it never changes whether the record is active. */
    fun save(item: T): T

    /** Soft-deletes the record; false when no active record of the user has that id. */
    fun deactivate(userId: UUID, id: UUID): Boolean

    /** Sum of the amounts of the active records of the user (zero when there are none). */
    fun sumActive(userId: UUID): BigDecimal
}
