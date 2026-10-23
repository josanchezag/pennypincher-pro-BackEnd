package com.pennypincher.pro.goals.infrastructure.web.mapper

import com.pennypincher.pro.goals.domain.model.GoalProjection
import com.pennypincher.pro.goals.domain.model.ProjectionPoint
import com.pennypincher.pro.goals.infrastructure.web.dto.GoalProjectionResponse
import com.pennypincher.pro.goals.infrastructure.web.dto.ProjectionPointResponse

/** Converts a domain [GoalProjection] into its HTTP representation. */
object GoalProjectionWebMapper {
    fun toResponse(projection: GoalProjection): GoalProjectionResponse = with(projection) {
        GoalProjectionResponse(
            goalId = goal.id,
            name = goal.name,
            level = goal.level,
            targetAmount = goal.targetAmount,
            currentNetWorth = currentNetWorth,
            progressAmount = progressAmount,
            progressPercentage = progressPercentage,
            remainingAmount = remainingAmount,
            monthlyInvestment = goal.monthlyInvestment,
            expectedAnnualReturn = goal.expectedAnnualReturn,
            targetYears = goal.targetYears,
            reachable = reachable,
            projectedMonthsToGoal = monthsToGoal,
            projectedYearsToGoal = yearsToGoal,
            projectedAchievementDate = achievementDate,
            projectionDataPoints = dataPoints.map(::toResponse),
        )
    }

    private fun toResponse(point: ProjectionPoint) =
        ProjectionPointResponse(point.month, point.date, point.projectedValue)
}
