package com.pennypincher.pro.goals.domain.service

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.FinancialGoalCommand
import com.pennypincher.pro.goals.domain.model.StandardGoals
import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GoalProjectionServiceTest {
    private val service = GoalProjectionService()

    @Test
    fun should_report_full_progress_and_month_zero_when_goal_is_already_reached() {
        val projection = service.project(goal(target = "605000000"), BigDecimal("700000000.00"), TODAY)

        assertEquals("100.00", projection.progressPercentage.toPlainString())
        assertEquals("605000000.00", projection.progressAmount.toPlainString())
        assertEquals("0.00", projection.remainingAmount.toPlainString())
        assertEquals("700000000.00", projection.currentNetWorth.toPlainString())
        assertTrue(projection.reachable)
        assertEquals(0, projection.monthsToGoal)
        assertEquals("0.00", projection.yearsToGoal?.toPlainString())
        assertEquals(CURRENT_MONTH, projection.achievementDate)
        assertEquals((0..MIN_HORIZON).toList(), projection.dataPoints.map { it.month })
    }

    @Test
    fun should_reach_goal_in_month_zero_when_net_worth_equals_target() {
        val projection = service.project(goal(target = "1000", monthly = "0"), BigDecimal("1000.00"), TODAY)

        assertEquals(0, projection.monthsToGoal)
        assertEquals("100.00", projection.progressPercentage.toPlainString())
    }

    @Test
    fun should_grow_only_by_returns_when_monthly_investment_is_zero() {
        val projection = service.project(goal(target = "200000000", monthly = "0"), BigDecimal("100000000"), TODAY)

        assertEquals(expectedMonths(100_000_000.0, 0.0, 200_000_000.0, ELEVEN_PERCENT), projection.monthsToGoal)
        assertEquals("50.00", projection.progressPercentage.toPlainString())
        val yearOne = projection.dataPoints[MONTHS_PER_YEAR].projectedValue
        assertEquals("111000000.00", yearOne.toPlainString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["0.03", "0.08", "0.11", "0.25", "1"])
    fun should_match_closed_form_month_count_when_return_rate_varies(annualReturn: String) {
        val projection = service.project(
            goal(target = "100000000", monthly = "1000000", annualReturn = annualReturn),
            BigDecimal("10000000"),
            TODAY,
        )

        val expected = expectedMonths(10_000_000.0, 1_000_000.0, 100_000_000.0, annualReturn.toDouble())
        assertEquals(expected, projection.monthsToGoal)
        assertEquals(expected, projection.dataPoints.last().month)
    }

    @Test
    fun should_reach_goal_sooner_when_return_rate_is_higher() {
        val months = listOf("0.03", "0.11", "0.25").map {
            service.project(goal(annualReturn = it), BigDecimal.ZERO, TODAY).monthsToGoal!!
        }

        assertEquals(months.sortedDescending(), months)
    }

    @Test
    fun should_clamp_progress_to_zero_and_count_the_debt_as_remaining_when_net_worth_is_negative() {
        val projection = service.project(
            goal(target = "100000000", monthly = "2000000"),
            BigDecimal("-50000000"),
            TODAY,
        )

        assertEquals("0.00", projection.progressAmount.toPlainString())
        assertEquals("0.00", projection.progressPercentage.toPlainString())
        assertEquals("150000000.00", projection.remainingAmount.toPlainString())
        assertEquals("-50000000.00", projection.dataPoints.first().projectedValue.toPlainString())
        assertEquals(expectedMonths(-50_000_000.0, 2_000_000.0, 100_000_000.0, ELEVEN_PERCENT), projection.monthsToGoal)
    }

    @Test
    fun should_be_unreachable_with_full_horizon_when_target_is_not_met_within_600_months() {
        listOf(
            service.project(goal(target = "4000000000000000", monthly = "1", annualReturn = "0.01"), ZERO, TODAY),
            service.project(goal(monthly = "0"), BigDecimal("-1000"), TODAY),
        ).forEach { projection ->
            assertFalse(projection.reachable)
            assertNull(projection.monthsToGoal)
            assertNull(projection.yearsToGoal)
            assertNull(projection.achievementDate)
            assertEquals(MAX_MONTHS + 1, projection.dataPoints.size)
            assertEquals(CURRENT_MONTH.plusMonths(MAX_MONTHS.toLong()), projection.dataPoints.last().date)
        }
    }

    @Test
    fun should_match_closed_form_month_count_for_each_spreadsheet_goal() {
        StandardGoals.ALL.forEach { standard ->
            val goal = standardGoal(standard)
            val fromZero = service.project(goal, ZERO, TODAY)
            val fromExcel = service.project(goal, EXCEL_NET_WORTH, TODAY)

            val target = standard.targetAmount.toDouble()
            val monthly = standard.monthlyInvestment.toDouble()
            assertEquals(expectedMonths(0.0, monthly, target, ELEVEN_PERCENT), fromZero.monthsToGoal, standard.name)
            assertEquals(
                expectedMonths(EXCEL_NET_WORTH.toDouble(), monthly, target, ELEVEN_PERCENT),
                fromExcel.monthsToGoal,
                standard.name,
            )
        }
    }

    @Test
    fun should_need_the_spreadsheet_month_counts_for_each_standard_goal() {
        val fromZero = StandardGoals.ALL.map { service.project(standardGoal(it), ZERO, TODAY).monthsToGoal }
        val fromExcel = StandardGoals.ALL.map { service.project(standardGoal(it), EXCEL_NET_WORTH, TODAY).monthsToGoal }

        assertEquals(listOf(176, 158, 147, 141), fromZero)
        assertEquals(listOf(31, 66, 98, 117), fromExcel)
    }

    @Test
    fun should_stay_exact_to_the_cent_after_600_months_of_compounding() {
        val projection = service.project(goal(monthly = "0"), BigDecimal("-76717227"), TODAY)

        assertEquals(MAX_MONTHS, projection.dataPoints.last().month)
        assertEquals("-14159301709.24", projection.dataPoints.last().projectedValue.toPlainString())
    }

    @Test
    fun should_report_progress_years_and_date_for_the_security_goal_from_the_spreadsheet_net_worth() {
        val projection = service.project(goal(), EXCEL_NET_WORTH, TODAY)
        val months = checkNotNull(projection.monthsToGoal)

        assertEquals("69.96", projection.progressPercentage.toPlainString())
        assertEquals("181717227.00", projection.remainingAmount.toPlainString())
        val years = BigDecimal(months).divide(BigDecimal(MONTHS_PER_YEAR), 2, RoundingMode.HALF_UP)
        assertEquals(years, projection.yearsToGoal)
        assertEquals(CURRENT_MONTH.plusMonths(months.toLong()), projection.achievementDate)
        assertTrue(projection.dataPoints[months].projectedValue >= BigDecimal("605000000"))
        assertTrue(projection.dataPoints[months - 1].projectedValue < BigDecimal("605000000"))
        assertEquals(months + 1, projection.dataPoints.size)
    }

    @Test
    fun should_date_points_on_the_first_of_each_month_and_compound_contributions_at_month_end() {
        val projection = service.project(goal(monthly = "1000", annualReturn = "0.11"), ZERO, TODAY)
        val points = projection.dataPoints

        assertEquals(LocalDate.parse("2026-10-01"), points[0].date)
        assertEquals(LocalDate.parse("2027-01-01"), points[3].date)
        assertEquals("0.00", points[0].projectedValue.toPlainString())
        assertEquals("1000.00", points[1].projectedValue.toPlainString())
        val rate = monthlyRate(ELEVEN_PERCENT)
        val expectedYearOne = 1000 * ((1 + rate).pow(MONTHS_PER_YEAR) - 1) / rate
        assertEquals(expectedYearOne, points[MONTHS_PER_YEAR].projectedValue.toDouble(), CENT)
    }

    @Test
    fun should_use_the_effective_monthly_equivalent_of_the_annual_return() {
        val rate = CompoundGrowth.monthlyRate(BigDecimal("0.11"))

        assertEquals(monthlyRate(ELEVEN_PERCENT), rate.toDouble(), RATE_TOLERANCE)
        val yearlyGrowth = BigDecimal.ONE.add(rate).pow(MONTHS_PER_YEAR).setScale(GROWTH_SCALE, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("1.110000000000"), yearlyGrowth)
    }

    /** Smallest n with FV(n) ≥ target, from the logarithm of the closed form instead of month by month. */
    private fun expectedMonths(presentValue: Double, monthly: Double, target: Double, annualReturn: Double): Int {
        if (presentValue >= target) return 0
        val rate = monthlyRate(annualReturn)
        return ceil(ln((target * rate + monthly) / (presentValue * rate + monthly)) / ln(1 + rate)).toInt()
    }

    private fun monthlyRate(annualReturn: Double): Double = (1 + annualReturn).pow(1.0 / MONTHS_PER_YEAR) - 1

    private fun standardGoal(standard: FinancialGoalCommand) = goal(
        target = standard.targetAmount.toPlainString(),
        monthly = standard.monthlyInvestment.toPlainString(),
        annualReturn = standard.expectedAnnualReturn.toPlainString(),
    )

    private fun goal(target: String = "605000000", monthly: String = "1470000", annualReturn: String = "0.11") =
        FinancialGoal(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            name = "Seguridad Financiera",
            level = GoalLevel.FINANCIAL_SECURITY,
            targetAmount = BigDecimal(target),
            monthlyInvestment = BigDecimal(monthly),
            expectedAnnualReturn = BigDecimal(annualReturn),
            targetYears = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )

    private companion object {
        val TODAY: LocalDate = LocalDate.parse("2026-10-23")
        val CURRENT_MONTH: LocalDate = LocalDate.parse("2026-10-01")
        val EXCEL_NET_WORTH = BigDecimal("423282773.00")
        val ZERO: BigDecimal = BigDecimal.ZERO
        const val ELEVEN_PERCENT = 0.11
        const val MONTHS_PER_YEAR = 12
        const val MIN_HORIZON = 12
        const val MAX_MONTHS = 600
        const val CENT = 0.01
        const val RATE_TOLERANCE = 1e-15
        const val GROWTH_SCALE = 12
    }
}
