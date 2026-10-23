package com.pennypincher.pro.goals.infrastructure.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Public representation of a goal; `targetYears` is always present, null when the goal has none. */
data class FinancialGoalResponse(
    val id: UUID,
    val name: String,
    val level: GoalLevel,
    val levelDescription: String,
    val targetAmount: BigDecimal,
    val monthlyInvestment: BigDecimal,
    val expectedAnnualReturn: BigDecimal,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val targetYears: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** One entry of `GET /financial-goals/levels`. */
data class GoalLevelResponse(val level: GoalLevel, val description: String) {
    companion object {
        fun of(level: GoalLevel) = GoalLevelResponse(level, level.description)
    }
}
