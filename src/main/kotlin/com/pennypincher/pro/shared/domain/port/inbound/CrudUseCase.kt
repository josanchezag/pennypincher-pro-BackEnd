package com.pennypincher.pro.shared.domain.port.inbound

import java.math.BigDecimal
import java.util.UUID

/** Managing the caller's own simple records. `userId` always comes from the access token. */
interface CrudUseCase<T, Cmd, C> {
    /** Active records, sorted by category and then by name; [category] narrows the list when given. */
    fun list(userId: UUID, category: C?): List<T>

    /** Sum of the amounts of the active records. */
    fun total(userId: UUID): BigDecimal

    /** One active record of the user; a missing, deleted or foreign record is reported as not found. */
    fun get(userId: UUID, id: UUID): T

    fun create(userId: UUID, command: Cmd): T

    fun update(userId: UUID, id: UUID, command: Cmd): T

    /** Soft-deletes the record. */
    fun delete(userId: UUID, id: UUID)
}
