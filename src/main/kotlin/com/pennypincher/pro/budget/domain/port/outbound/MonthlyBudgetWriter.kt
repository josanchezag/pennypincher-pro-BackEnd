package com.pennypincher.pro.budget.domain.port.outbound

import com.pennypincher.pro.budget.domain.model.MonthlyBudget

/** Write side of the monthly budget persistence; every method is scoped by the budget's user. */
interface MonthlyBudgetWriter {
    /**
     * Inserts [budget] unless its user already has a budget for that month; false in that case. Atomic, so
     * concurrent inserts of the same month store exactly one row.
     */
    fun insertIfAbsent(budget: MonthlyBudget): Boolean

    /**
     * Replaces the settings and figures of the user's existing budget and returns it as stored (the database
     * sets `updatedAt`); null when the user has no budget with that id.
     */
    fun update(budget: MonthlyBudget): MonthlyBudget?
}
