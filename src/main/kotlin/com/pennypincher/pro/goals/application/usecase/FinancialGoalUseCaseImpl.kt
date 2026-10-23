package com.pennypincher.pro.goals.application.usecase

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.FinancialGoalCommand
import com.pennypincher.pro.goals.domain.model.GoalProjection
import com.pennypincher.pro.goals.domain.model.StandardGoals
import com.pennypincher.pro.goals.domain.port.inbound.FinancialGoalUseCase
import com.pennypincher.pro.goals.domain.port.outbound.FinancialGoalRepository
import com.pennypincher.pro.goals.domain.service.GoalProjectionService
import com.pennypincher.pro.networth.domain.port.inbound.NetWorthUseCase
import com.pennypincher.pro.shared.application.usecase.AbstractCrudUseCase
import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.model.RecordName
import com.pennypincher.pro.shared.domain.validation.FieldValidator
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Financial goal use cases. The name is trimmed and keeps 1 to 150 characters, the target must be positive,
 * the monthly investment non-negative (both COP amounts), the expected annual return a fraction in (0, 1]
 * with at most 4 decimals, and the optional target years between 1 and 100; an update without target years
 * clears them. Lists follow the level order, then the target amount and then the name ignoring case.
 * Deleting removes the goal for good (the repository decides how a record is deleted). Projections use the
 * net worth read live from [netWorthUseCase] and today's date from [clock].
 */
class FinancialGoalUseCaseImpl(
    repository: FinancialGoalRepository,
    private val netWorthUseCase: NetWorthUseCase,
    private val projectionService: GoalProjectionService,
    private val clock: Clock,
) : AbstractCrudUseCase<FinancialGoal, FinancialGoalCommand, Nothing>(repository, clock),
    FinancialGoalUseCase {
    override val resourceName = "Financial goal"

    override val ordering: Comparator<FinancialGoal> = compareBy<FinancialGoal>({ it.level }, { it.targetAmount })
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }

    override fun validate(command: FinancialGoalCommand) = FieldValidator()
        .requireText(RecordName.FIELD, command.name, RecordName.MAX_LENGTH)
        .requirePositiveAmount(TARGET_AMOUNT_FIELD, command.targetAmount)
        .requireNonNegativeAmount(MONTHLY_INVESTMENT_FIELD, command.monthlyInvestment)
        .requireAnnualReturn(command.expectedAnnualReturn)
        .require(
            command.targetYears == null || command.targetYears in TARGET_YEARS_RANGE,
            TARGET_YEARS_FIELD,
            YEARS_RULE,
        )
        .validate()

    override fun build(userId: UUID, command: FinancialGoalCommand, now: Instant) = FinancialGoal(
        id = UUID.randomUUID(),
        userId = userId,
        name = command.name.trim(),
        level = command.level,
        targetAmount = Money.of(command.targetAmount),
        monthlyInvestment = Money.of(command.monthlyInvestment),
        expectedAnnualReturn = command.expectedAnnualReturn.setScale(ANNUAL_RETURN_SCALE),
        targetYears = command.targetYears,
        createdAt = now,
        updatedAt = now,
    )

    override fun applyChanges(current: FinancialGoal, command: FinancialGoalCommand, now: Instant) =
        build(current.userId, command, now).copy(id = current.id, createdAt = current.createdAt)

    override fun createDefaults(userId: UUID): List<FinancialGoal> {
        val usedLevels = list(userId, null).map(FinancialGoal::level).toSet()
        StandardGoals.ALL.filterNot { it.level in usedLevels }.forEach { create(userId, it) }
        return list(userId, null)
    }

    override fun getProjection(userId: UUID, goalId: UUID): GoalProjection =
        project(userId, listOf(get(userId, goalId))).single()

    override fun getAllProjections(userId: UUID): List<GoalProjection> = project(userId, list(userId, null))

    private fun project(userId: UUID, goals: List<FinancialGoal>): List<GoalProjection> {
        if (goals.isEmpty()) return emptyList()
        val netWorth = netWorthUseCase.getCurrentNetWorth(userId).netWorth
        val today = LocalDate.now(clock)
        return goals.map { projectionService.project(it, netWorth, today) }
    }

    private fun FieldValidator.requireAnnualReturn(value: BigDecimal): FieldValidator = when {
        value.signum() <= 0 || value > BigDecimal.ONE -> require(false, ANNUAL_RETURN_FIELD, ANNUAL_RETURN_RANGE)
        value.stripTrailingZeros().scale() > ANNUAL_RETURN_SCALE ->
            require(false, ANNUAL_RETURN_FIELD, ANNUAL_RETURN_PRECISION)
        else -> this
    }

    private companion object {
        const val TARGET_AMOUNT_FIELD = "targetAmount"
        const val MONTHLY_INVESTMENT_FIELD = "monthlyInvestment"
        const val ANNUAL_RETURN_FIELD = "expectedAnnualReturn"
        const val TARGET_YEARS_FIELD = "targetYears"
        const val ANNUAL_RETURN_SCALE = 4
        const val MIN_TARGET_YEARS = 1
        const val MAX_TARGET_YEARS = 100
        val TARGET_YEARS_RANGE = MIN_TARGET_YEARS..MAX_TARGET_YEARS
        const val ANNUAL_RETURN_RANGE = "must be greater than 0 and at most 1"
        const val ANNUAL_RETURN_PRECISION = "must have at most $ANNUAL_RETURN_SCALE decimal places"
        const val YEARS_RULE = "must be between $MIN_TARGET_YEARS and $MAX_TARGET_YEARS"
    }
}
