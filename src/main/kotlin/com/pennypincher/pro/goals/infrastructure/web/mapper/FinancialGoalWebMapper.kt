package com.pennypincher.pro.goals.infrastructure.web.mapper

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.FinancialGoalCommand
import com.pennypincher.pro.goals.infrastructure.web.dto.FinancialGoalRequest
import com.pennypincher.pro.goals.infrastructure.web.dto.FinancialGoalResponse

/** Converts between the financial goal HTTP DTOs and the domain. */
object FinancialGoalWebMapper {
    /** Expects a request that already passed bean validation, so no required field is null. */
    fun toCommand(request: FinancialGoalRequest): FinancialGoalCommand = FinancialGoalCommand(
        name = checkNotNull(request.name),
        level = checkNotNull(request.level),
        targetAmount = checkNotNull(request.targetAmount),
        monthlyInvestment = checkNotNull(request.monthlyInvestment),
        expectedAnnualReturn = checkNotNull(request.expectedAnnualReturn),
        targetYears = request.targetYears,
    )

    fun toResponse(goal: FinancialGoal): FinancialGoalResponse = FinancialGoalResponse(
        id = goal.id,
        name = goal.name,
        level = goal.level,
        levelDescription = goal.level.description,
        targetAmount = goal.targetAmount,
        monthlyInvestment = goal.monthlyInvestment,
        expectedAnnualReturn = goal.expectedAnnualReturn,
        targetYears = goal.targetYears,
        createdAt = goal.createdAt,
        updatedAt = goal.updatedAt,
    )
}
