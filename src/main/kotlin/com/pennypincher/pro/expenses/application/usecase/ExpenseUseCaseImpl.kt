package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.expenses.domain.model.ExpenseCommand
import com.pennypincher.pro.expenses.domain.model.ExpenseSummary
import com.pennypincher.pro.expenses.domain.port.inbound.ExpenseUseCase
import com.pennypincher.pro.expenses.domain.port.outbound.ExpenseRepository
import com.pennypincher.pro.shared.application.usecase.AbstractCrudUseCase
import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.model.RecordName
import com.pennypincher.pro.shared.domain.validation.FieldValidator
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Expense use cases, shared by fixed and variable expenses: the name is trimmed, the monthly amount must
 * be a non-negative COP amount, lists are ordered by category and then by name ignoring case, and the
 * summary comes from per-category totals computed by the database.
 */
class ExpenseUseCaseImpl<C : Enum<C>>(
    private val repository: ExpenseRepository<C>,
    clock: Clock,
    override val resourceName: String,
) : AbstractCrudUseCase<Expense<C>, ExpenseCommand<C>, C>(repository, clock),
    ExpenseUseCase<C> {
    override val ordering: Comparator<Expense<C>> =
        compareBy<Expense<C>> { it.category }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }

    override fun validate(command: ExpenseCommand<C>) = FieldValidator()
        .requireText(RecordName.FIELD, command.name, RecordName.MAX_LENGTH)
        .requireNonNegativeAmount(MONTHLY_AMOUNT_FIELD, command.monthlyAmount)
        .validate()

    override fun build(userId: UUID, command: ExpenseCommand<C>, now: Instant) = Expense(
        id = UUID.randomUUID(),
        userId = userId,
        name = command.name.trim(),
        monthlyAmount = Money.of(command.monthlyAmount),
        category = command.category,
        createdAt = now,
        updatedAt = now,
    )

    override fun applyChanges(current: Expense<C>, command: ExpenseCommand<C>, now: Instant) = current.copy(
        name = command.name.trim(),
        monthlyAmount = Money.of(command.monthlyAmount),
        category = command.category,
        updatedAt = now,
    )

    override fun summary(userId: UUID): ExpenseSummary<C> = ExpenseSummary.of(repository.totalsByCategory(userId))

    private companion object {
        const val MONTHLY_AMOUNT_FIELD = "monthlyAmount"
    }
}
