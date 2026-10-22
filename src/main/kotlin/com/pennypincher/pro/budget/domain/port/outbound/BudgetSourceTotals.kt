package com.pennypincher.pro.budget.domain.port.outbound

import com.pennypincher.pro.budget.domain.model.BudgetSourceAmounts
import java.util.UUID

/** Read-only port that sums every source of a budget for one user in a single consistent read. */
interface BudgetSourceTotals {
    fun forUser(userId: UUID): BudgetSourceAmounts
}
