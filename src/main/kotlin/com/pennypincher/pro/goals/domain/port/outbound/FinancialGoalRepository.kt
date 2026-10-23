package com.pennypincher.pro.goals.domain.port.outbound

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.shared.domain.port.outbound.UserScopedCrudRepository

/** Persistence port for financial goals: no category, and [delete] removes the row for good. */
interface FinancialGoalRepository : UserScopedCrudRepository<FinancialGoal, Nothing>
