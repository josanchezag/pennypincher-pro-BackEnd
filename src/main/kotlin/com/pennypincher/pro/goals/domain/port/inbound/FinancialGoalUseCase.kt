package com.pennypincher.pro.goals.domain.port.inbound

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.FinancialGoalCommand
import com.pennypincher.pro.goals.domain.model.GoalProjection
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase
import java.util.UUID

/**
 * Managing the caller's financial goals: CRUD without category (hard delete), the standard goals and the
 * projection of each goal against the live net worth.
 */
interface FinancialGoalUseCase : CrudUseCase<FinancialGoal, FinancialGoalCommand, Nothing> {
    /**
     * Creates the standard goal of every level that has no goal of the user yet, leaving the others untouched,
     * and returns all the user's goals in display order. Calling it again changes nothing.
     */
    fun createDefaults(userId: UUID): List<FinancialGoal>

    /** Progress and projection of one goal of the user against the current net worth. */
    fun getProjection(userId: UUID, goalId: UUID): GoalProjection

    /** [getProjection] of every goal of the user, in the order of the goal list. */
    fun getAllProjections(userId: UUID): List<GoalProjection>
}
