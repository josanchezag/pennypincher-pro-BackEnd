package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.BalanceAccount
import com.pennypincher.pro.expenses.domain.model.BalanceAccountCommand
import com.pennypincher.pro.expenses.domain.model.BalanceTotals
import com.pennypincher.pro.expenses.domain.port.inbound.BalanceAccountUseCase
import com.pennypincher.pro.expenses.domain.port.outbound.BalanceAccountRepository
import com.pennypincher.pro.shared.application.usecase.AbstractCrudUseCase
import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.model.RecordName
import com.pennypincher.pro.shared.domain.validation.FieldValidator
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID

/**
 * Rules shared by the [BalanceAccount] use cases: the name is trimmed and keeps 1 to 150 characters, every
 * given amount must be a non-negative COP amount, lists are ordered by name ignoring case, and the summary
 * has two decimal places. Subclasses name their amount fields and build the records.
 */
abstract class AbstractBalanceAccountUseCase<T : BalanceAccount, Cmd : BalanceAccountCommand>(
    private val repository: BalanceAccountRepository<T>,
    clock: Clock,
) : AbstractCrudUseCase<T, Cmd, Nothing>(repository, clock),
    BalanceAccountUseCase<T, Cmd> {
    final override val ordering: Comparator<T> = compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }

    /** Hook: the amounts of [command] keyed by their field name, in report order; null means not given. */
    protected abstract fun amountsOf(command: Cmd): Map<String, BigDecimal?>

    final override fun validate(command: Cmd) {
        val validator = FieldValidator().requireText(RecordName.FIELD, command.name, RecordName.MAX_LENGTH)
        amountsOf(command).forEach { (field, amount) -> amount?.let { validator.requireNonNegativeAmount(field, it) } }
        validator.validate()
    }

    final override fun summary(userId: UUID): BalanceTotals {
        val totals = repository.summarizeActive(userId)
        return BalanceTotals(Money.of(totals.totalMonthlyAmount), Money.of(totals.totalBalance), totals.count)
    }
}
