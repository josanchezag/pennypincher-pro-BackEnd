package com.pennypincher.pro.goals.domain.service

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.GoalProjection
import com.pennypincher.pro.goals.domain.model.ProjectionPoint
import com.pennypincher.pro.shared.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Projects a goal from the current net worth with [CompoundGrowth]. The goal is reached in the first month
 * whose projected value is at least the target (month 0 when the net worth already covers it), looking at
 * most [MAX_MONTHS] months ahead. The data points run from month 0 to that month, but cover at least
 * [MIN_HORIZON_MONTHS] months, or all [MAX_MONTHS] when the goal is not reached. Amounts and percentages are
 * rounded half up to two decimals. Pure Kotlin: the caller supplies the net worth and today's date.
 */
class GoalProjectionService {
    fun project(goal: FinancialGoal, currentNetWorth: BigDecimal, today: LocalDate): GoalProjection {
        val target = goal.targetAmount
        val values = CompoundGrowth.futureValues(
            currentNetWorth,
            goal.monthlyInvestment,
            goal.expectedAnnualReturn,
            MAX_MONTHS,
        )
        val months = values.indexOfFirst { it >= target }.takeIf { it >= 0 }
        val horizon = (months ?: MAX_MONTHS).coerceIn(MIN_HORIZON_MONTHS, MAX_MONTHS)
        val currentMonth = today.withDayOfMonth(1)
        val progress = currentNetWorth.coerceIn(BigDecimal.ZERO, target)
        return GoalProjection(
            goal = goal,
            currentNetWorth = rounded(currentNetWorth),
            progressAmount = rounded(progress),
            progressPercentage = progress.multiply(PERCENT).divide(target, Money.SCALE, RoundingMode.HALF_UP),
            remainingAmount = rounded(target.subtract(currentNetWorth).max(BigDecimal.ZERO)),
            monthsToGoal = months,
            yearsToGoal = months?.let { BigDecimal(it).divide(MONTHS_PER_YEAR, Money.SCALE, RoundingMode.HALF_UP) },
            achievementDate = months?.let { currentMonth.plusMonths(it.toLong()) },
            dataPoints = values.take(horizon + 1).mapIndexed { month, value ->
                ProjectionPoint(month, currentMonth.plusMonths(month.toLong()), rounded(value))
            },
        )
    }

    private fun rounded(amount: BigDecimal): BigDecimal = amount.setScale(Money.SCALE, RoundingMode.HALF_UP)

    companion object {
        /** Furthest month looked at: 50 years. */
        const val MAX_MONTHS = 600

        /** Fewest months the data points cover after month 0. */
        const val MIN_HORIZON_MONTHS = 12

        private val MONTHS_PER_YEAR = BigDecimal(CompoundGrowth.MONTHS_PER_YEAR)
        private val PERCENT = BigDecimal(100)
    }
}
