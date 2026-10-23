package com.pennypincher.pro.goals.domain.model

import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import com.pennypincher.pro.shared.domain.model.OwnedResource
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * A financial independence milestone of the user: the amount to reach, what the user invests each month
 * towards it and the yearly return expected on that money (a fraction, 0.11 for 11 %). Goals are hard
 * deleted.
 */
data class FinancialGoal(
    override val id: UUID,
    override val userId: UUID,
    val name: String,
    val level: GoalLevel,
    val targetAmount: BigDecimal,
    val monthlyInvestment: BigDecimal,
    val expectedAnnualReturn: BigDecimal,
    val targetYears: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
) : OwnedResource

/** Data a user supplies to create or update a goal; `targetYears` is optional. */
data class FinancialGoalCommand(
    val name: String,
    val level: GoalLevel,
    val targetAmount: BigDecimal,
    val monthlyInvestment: BigDecimal,
    val expectedAnnualReturn: BigDecimal,
    val targetYears: Int? = null,
)
