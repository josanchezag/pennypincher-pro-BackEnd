package com.pennypincher.pro.shared.domain.port.outbound

import java.util.UUID

/**
 * Persistence port shared by the simple per-user records (assets, credit cards, goals, ...). Every method is
 * scoped by `userId`, and only active rows are visible: rows that were not soft-deleted, or every stored row
 * for the kinds of record that are hard deleted. [C] is the category type, or `Nothing` for records without
 * category.
 */
interface UserScopedCrudRepository<T, C> {
    /** Active records of the user, optionally restricted to one [category]; the order is not defined. */
    fun findAllActive(userId: UUID, category: C?): List<T>

    fun findActiveById(userId: UUID, id: UUID): T?

    /** Stores a new record. */
    fun insert(item: T): T

    /**
     * Stores the new state of an existing record of the user; it never changes whether the record is active
     * and never brings back a removed row. Null when the row no longer exists (a hard delete that happened
     * after the record was read).
     */
    fun update(item: T): T?

    /**
     * Deletes the record the way its kind requires: a soft delete for most records, removing the row for the
     * hard-deleted ones. False when no active record of the user has that id.
     */
    fun delete(userId: UUID, id: UUID): Boolean
}
