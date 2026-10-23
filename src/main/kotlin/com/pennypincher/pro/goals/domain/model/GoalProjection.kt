package com.pennypincher.pro.goals.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Progress of a [goal] against the user's current net worth and when the goal is reached if the monthly
 * investment keeps compounding at the expected return. The month fields are null when the goal is not
 * reached within the projection limit.
 */
data class GoalProjection(
    val goal: FinancialGoal,
    val currentNetWorth: BigDecimal,
    val progressAmount: BigDecimal,
    val progressPercentage: BigDecimal,
    val remainingAmount: BigDecimal,
    val monthsToGoal: Int?,
    val yearsToGoal: BigDecimal?,
    val achievementDate: LocalDate?,
    val dataPoints: List<ProjectionPoint>,
) {
    val reachable: Boolean get() = monthsToGoal != null
}

/** Projected value [month] months from the current month, on the first day of that month. */
data class ProjectionPoint(val month: Int, val date: LocalDate, val projectedValue: BigDecimal)
