package com.pennypincher.pro.budget.application.usecase

import com.pennypincher.pro.budget.domain.exception.BudgetAlreadyExistsException
import com.pennypincher.pro.budget.domain.model.BudgetFigures
import com.pennypincher.pro.budget.domain.model.BudgetPeriod
import com.pennypincher.pro.budget.domain.model.BudgetSettings
import com.pennypincher.pro.budget.domain.model.CreateBudgetCommand
import com.pennypincher.pro.budget.domain.model.MonthlyBudget
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import com.pennypincher.pro.budget.domain.port.inbound.MonthlyBudgetUseCase
import com.pennypincher.pro.budget.domain.port.outbound.BudgetSourceTotals
import com.pennypincher.pro.budget.domain.port.outbound.MonthlyBudgetReader
import com.pennypincher.pro.budget.domain.port.outbound.MonthlyBudgetWriter
import com.pennypincher.pro.budget.domain.service.BudgetCalculationService
import com.pennypincher.pro.budget.domain.service.BudgetComponentStrategy
import com.pennypincher.pro.shared.domain.exception.ResourceNotFoundException
import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.validation.FieldValidator
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Monthly budget use cases. The figures come from one consistent read of the user's active income, debt,
 * expense and savings records, split by one [BudgetComponentStrategy] per component; totals that do not
 * fit the money format are rejected.
 */
class MonthlyBudgetUseCaseImpl(
    private val budgetReader: MonthlyBudgetReader,
    private val budgetWriter: MonthlyBudgetWriter,
    private val sourceTotals: BudgetSourceTotals,
    strategies: List<BudgetComponentStrategy>,
    private val calculator: BudgetCalculationService,
    private val clock: Clock,
) : MonthlyBudgetUseCase {
    private val strategies = strategies.associateBy { it.component }

    init {
        require(this.strategies.keys == BudgetComponent.entries.toSet() && strategies.size == this.strategies.size) {
            "Exactly one strategy per budget component is required"
        }
    }

    override fun create(userId: UUID, command: CreateBudgetCommand): MonthlyBudget {
        val period = BudgetPeriod.of(command.year, command.month)
        val settings = normalized(command.settings)
        val now = now()
        val budget = MonthlyBudget(UUID.randomUUID(), userId, period, settings, calculate(userId), now, now)
        if (!budgetWriter.insertIfAbsent(budget)) throw BudgetAlreadyExistsException(period)
        return budget
    }

    override fun list(userId: UUID, year: Int?): List<MonthlyBudget> {
        year?.let(BudgetPeriod::requireValidYear)
        return budgetReader.findAll(userId, year).sortedWith(MonthlyBudget.DISPLAY_ORDER)
    }

    override fun get(userId: UUID, year: Int, month: Int): MonthlyBudget =
        budgetReader.findByPeriod(userId, BudgetPeriod.of(year, month)) ?: throw notFound()

    override fun update(userId: UUID, id: UUID, settings: BudgetSettings): MonthlyBudget {
        val normalized = normalized(settings)
        return recalculate(owned(userId, id).copy(settings = normalized))
    }

    override fun recalculate(userId: UUID, id: UUID): MonthlyBudget = recalculate(owned(userId, id))

    private fun recalculate(budget: MonthlyBudget): MonthlyBudget =
        budgetWriter.update(budget.copy(figures = calculate(budget.userId), updatedAt = now())) ?: throw notFound()

    private fun owned(userId: UUID, id: UUID): MonthlyBudget = budgetReader.findById(userId, id) ?: throw notFound()

    private fun calculate(userId: UUID): BudgetFigures {
        val sources = sourceTotals.forUser(userId)
        val amounts = strategies.mapValues { (_, strategy) -> strategy.monthlyAmount(sources) }
        val figures = calculator.calculate(sources.totalIncome, amounts)
        val validator = FieldValidator()
            .requireNonNegativeAmount(TOTAL_INCOME_FIELD, figures.totalIncome)
            .requireNonNegativeAmount(TOTAL_EXPENSES_FIELD, figures.totalExpenses)
        figures.lines.forEach { validator.requireNonNegativeAmount("$COMPONENTS_FIELD.${it.component}", it.amount) }
        validator.validate()
        return figures
    }

    /** Validates the manual values and gives the exchange rate the money scale. */
    private fun normalized(settings: BudgetSettings): BudgetSettings {
        val rate = settings.exchangeRate
        val workDays = settings.workDays
        FieldValidator()
            .require(rate == null || rate.signum() > 0 && Money.fits(rate), EXCHANGE_RATE_FIELD, EXCHANGE_RATE_RULE)
            .require(workDays == null || workDays in WORK_DAYS_RANGE, WORK_DAYS_FIELD, WORK_DAYS_RULE)
            .validate()
        return settings.copy(exchangeRate = rate?.let(Money::of))
    }

    /**
     * Current time at the precision PostgreSQL stores, so a newly inserted budget is returned exactly as stored.
     * On update the database trigger sets `updated_at`, and the writer returns that stored value instead.
     */
    private fun now(): Instant = Instant.now(clock).truncatedTo(ChronoUnit.MICROS)

    private fun notFound() = ResourceNotFoundException(RESOURCE_NAME)

    private companion object {
        const val RESOURCE_NAME = "Monthly budget"
        const val TOTAL_INCOME_FIELD = "totalIncome"
        const val TOTAL_EXPENSES_FIELD = "totalExpenses"
        const val COMPONENTS_FIELD = "components"
        const val EXCHANGE_RATE_FIELD = "exchangeRate"
        const val WORK_DAYS_FIELD = "workDays"
        const val EXCHANGE_RATE_RULE =
            "must be greater than 0 with at most ${Money.MAX_INTEGER_DIGITS} integer digits and " +
                "${Money.SCALE} decimal places"
        val WORK_DAYS_RANGE = BudgetSettings.MIN_WORK_DAYS..BudgetSettings.MAX_WORK_DAYS
        val WORK_DAYS_RULE = "must be between ${BudgetSettings.MIN_WORK_DAYS} and ${BudgetSettings.MAX_WORK_DAYS}"
    }
}
