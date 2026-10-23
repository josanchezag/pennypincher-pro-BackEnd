package com.pennypincher.pro.goals.domain.port.inbound

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.FinancialGoalCommand
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase
import java.util.UUID

/** Managing the caller's financial goals: CRUD without category (hard delete) and the standard goals. */
interface FinancialGoalUseCase : CrudUseCase<FinancialGoal, FinancialGoalCommand, Nothing> {
    /**
     * Creates the standard goal of every level that has no goal of the user yet, leaving the others untouched,
     * and returns all the user's goals in display order. Calling it again changes nothing.
     */
    fun createDefaults(userId: UUID): List<FinancialGoal>
}
