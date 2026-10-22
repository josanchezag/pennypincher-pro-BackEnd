package com.pennypincher.pro.shared.domain.port.inbound

import java.util.UUID

/**
 * Managing the caller's own simple records. `userId` always comes from the access token. [C] is the
 * category that can narrow [list]; records without category use `Nothing`, so the filter is always null.
 */
interface CrudUseCase<T, Cmd, C> {
    /** Active records in display order; [category] narrows the list when given. */
    fun list(userId: UUID, category: C?): List<T>

    /** One active record of the user; a missing, deleted or foreign record is reported as not found. */
    fun get(userId: UUID, id: UUID): T

    fun create(userId: UUID, command: Cmd): T

    fun update(userId: UUID, id: UUID, command: Cmd): T

    /** Soft-deletes the record. */
    fun delete(userId: UUID, id: UUID)
}
