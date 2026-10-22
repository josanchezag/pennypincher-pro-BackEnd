package com.pennypincher.pro.expenses.domain.port.outbound

import com.pennypincher.pro.expenses.domain.model.BalanceAccount
import com.pennypincher.pro.expenses.domain.model.BalanceTotals
import com.pennypincher.pro.shared.domain.port.outbound.UserScopedCrudRepository
import java.util.UUID

/** Persistence port for one kind of [BalanceAccount]; records have no category. */
interface BalanceAccountRepository<T : BalanceAccount> : UserScopedCrudRepository<T, Nothing> {
    /** Sums of the monthly amounts and balances of the user's active records, and their count. */
    fun summarizeActive(userId: UUID): BalanceTotals
}
