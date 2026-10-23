package com.pennypincher.pro.goals.infrastructure.web.dto

import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/**
 * Body of `POST /financial-goals` and `PUT /financial-goals/{id}`; `targetYears` is optional. The business
 * rules are checked by the use case.
 */
data class FinancialGoalRequest(
    @field:NotNull(message = RequestMessages.REQUIRED)
    val name: String? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val level: GoalLevel? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val targetAmount: BigDecimal? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val monthlyInvestment: BigDecimal? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val expectedAnnualReturn: BigDecimal? = null,
    val targetYears: Int? = null,
)
