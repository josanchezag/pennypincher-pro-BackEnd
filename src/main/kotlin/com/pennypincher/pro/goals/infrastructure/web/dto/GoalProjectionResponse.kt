package com.pennypincher.pro.goals.infrastructure.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Body of `GET /financial-goals/{id}/projection` and of each item of `/all-projections`. The nullable fields
 * are always present: `targetYears` is null when the goal has none, and the `projected*` fields when the goal
 * is not reached within 600 months (`reachable` false).
 */
data class GoalProjectionResponse(
    val goalId: UUID,
    val name: String,
    val level: GoalLevel,
    val targetAmount: BigDecimal,
    val currentNetWorth: BigDecimal,
    val progressAmount: BigDecimal,
    val progressPercentage: BigDecimal,
    val remainingAmount: BigDecimal,
    val monthlyInvestment: BigDecimal,
    val expectedAnnualReturn: BigDecimal,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val targetYears: Int?,
    val reachable: Boolean,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val projectedMonthsToGoal: Int?,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val projectedYearsToGoal: BigDecimal?,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val projectedAchievementDate: LocalDate?,
    val projectionDataPoints: List<ProjectionPointResponse>,
)

/** One month of the projected trajectory; month 0 is the current net worth. */
data class ProjectionPointResponse(val month: Int, val date: LocalDate, val projectedValue: BigDecimal)
