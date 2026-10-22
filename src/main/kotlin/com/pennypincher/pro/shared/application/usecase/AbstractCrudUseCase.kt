package com.pennypincher.pro.shared.application.usecase

import com.pennypincher.pro.shared.domain.exception.ResourceNotFoundException
import com.pennypincher.pro.shared.domain.model.OwnedResource
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase
import com.pennypincher.pro.shared.domain.port.outbound.UserScopedCrudRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Template Method for the per-user CRUD records. The flow (validation, ownership check, soft delete,
 * sorting) is fixed here; subclasses supply the hooks that differ per record type. A record of another
 * user is reported exactly like a missing one.
 */
abstract class AbstractCrudUseCase<T : OwnedResource, Cmd, C>(
    private val repository: UserScopedCrudRepository<T, C>,
    private val clock: Clock,
) : CrudUseCase<T, Cmd, C> {
    /** Name used in the not-found message, such as "Asset". */
    protected abstract val resourceName: String

    /** Order of [list], such as by category and then by name. */
    protected abstract val ordering: Comparator<T>

    /** Hook: rejects a command that breaks the business rules. */
    protected abstract fun validate(command: Cmd)

    /** Hook: builds a new record from a valid command. */
    protected abstract fun build(userId: UUID, command: Cmd, now: Instant): T

    /** Hook: applies a valid command to an existing record. */
    protected abstract fun applyChanges(current: T, command: Cmd, now: Instant): T

    final override fun list(userId: UUID, category: C?): List<T> =
        repository.findAllActive(userId, category).sortedWith(ordering)

    final override fun get(userId: UUID, id: UUID): T =
        repository.findActiveById(userId, id) ?: throw ResourceNotFoundException(resourceName)

    final override fun create(userId: UUID, command: Cmd): T {
        validate(command)
        return repository.save(build(userId, command, Instant.now(clock)))
    }

    final override fun update(userId: UUID, id: UUID, command: Cmd): T {
        validate(command)
        return repository.save(applyChanges(get(userId, id), command, Instant.now(clock)))
    }

    final override fun delete(userId: UUID, id: UUID) {
        if (!repository.deactivate(userId, id)) throw ResourceNotFoundException(resourceName)
    }
}
