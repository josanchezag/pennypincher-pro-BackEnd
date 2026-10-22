package com.pennypincher.pro.budget.domain.port.inbound

import com.pennypincher.pro.budget.domain.model.BudgetSettings
import com.pennypincher.pro.budget.domain.model.CreateBudgetCommand
import com.pennypincher.pro.budget.domain.model.MonthlyBudget
import java.util.UUID

/**
 * Monthly budgets of the caller. Their figures are calculated from the current income sources and expense
 * records when the budget is created, updated or recalculated. `userId` always comes from the access token.
 */
interface MonthlyBudgetUseCase {
    /** Creates the budget of a month; fails with a conflict when that month already has one. */
    fun create(userId: UUID, command: CreateBudgetCommand): MonthlyBudget

    /** Budgets, newest month first; only those of [year] when given. */
    fun list(userId: UUID, year: Int?): List<MonthlyBudget>

    fun get(userId: UUID, year: Int, month: Int): MonthlyBudget

    /** Replaces the manual [settings] (null clears a value) and recalculates the figures. */
    fun update(userId: UUID, id: UUID, settings: BudgetSettings): MonthlyBudget

    /** Recalculates the figures from the current income and expense records. */
    fun recalculate(userId: UUID, id: UUID): MonthlyBudget
}
