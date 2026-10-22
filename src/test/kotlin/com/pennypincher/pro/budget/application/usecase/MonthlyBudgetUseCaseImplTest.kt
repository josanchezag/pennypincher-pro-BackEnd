package com.pennypincher.pro.budget.application.usecase

import com.pennypincher.pro.budget.domain.exception.BudgetAlreadyExistsException
import com.pennypincher.pro.budget.domain.model.BudgetPeriod
import com.pennypincher.pro.budget.domain.model.BudgetSettings
import com.pennypincher.pro.budget.domain.model.BudgetSourceAmounts
import com.pennypincher.pro.budget.domain.model.CreateBudgetCommand
import com.pennypincher.pro.budget.domain.model.MonthlyBudget
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import com.pennypincher.pro.budget.domain.port.outbound.BudgetSourceTotals
import com.pennypincher.pro.budget.domain.port.outbound.MonthlyBudgetReader
import com.pennypincher.pro.budget.domain.port.outbound.MonthlyBudgetWriter
import com.pennypincher.pro.budget.domain.service.BudgetCalculationService
import com.pennypincher.pro.budget.domain.service.BudgetComponentStrategy
import com.pennypincher.pro.shared.domain.exception.ErrorCode
import com.pennypincher.pro.shared.domain.exception.ResourceNotFoundException
import com.pennypincher.pro.shared.domain.exception.ValidationException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MonthlyBudgetUseCaseImplTest {
    private val userId = UUID.randomUUID()
    private val reader = mockk<MonthlyBudgetReader>()
    private val writer = mockk<MonthlyBudgetWriter>()
    private var income = BigDecimal("1000.00")
    private val sourceTotals = mockk<BudgetSourceTotals> {
        every { forUser(userId) } answers { sourcesWith(income) }
    }
    private val amounts = mutableMapOf(
        BudgetComponent.LOANS to BigDecimal("200.00"),
        BudgetComponent.SAVINGS to BigDecimal("100.00"),
    )
    private val useCase = useCaseWith(BudgetComponent.entries.map(::strategy))

    @Test
    fun should_insert_calculated_budget_when_month_has_none() {
        val inserted = slot<MonthlyBudget>()
        every { writer.insertIfAbsent(capture(inserted)) } returns true

        val created = useCase.create(userId, CreateBudgetCommand(2026, 10, BudgetSettings(BigDecimal("3300"), 22)))

        assertEquals(inserted.captured, created)
        assertEquals(userId to BudgetPeriod(2026, 10), created.userId to created.period)
        assertEquals(BudgetSettings(BigDecimal("3300.00"), 22), created.settings)
        assertEquals("3300.00", created.settings.exchangeRate?.toPlainString())
        assertEquals("1000.00", created.figures.totalIncome.toPlainString())
        assertEquals("200.00", created.figures.totalExpenses.toPlainString())
        assertEquals("800.00", created.figures.monthlyCashFlow.toPlainString())
        assertEquals("20.0000", created.figures.line(BudgetComponent.LOANS).percentage.toPlainString())
        assertEquals(NOW to NOW, created.createdAt to created.updatedAt)
    }

    @Test
    fun should_throw_conflict_when_month_already_has_a_budget() {
        every { writer.insertIfAbsent(any()) } returns false

        val exception = assertThrows<BudgetAlreadyExistsException> {
            useCase.create(userId, CreateBudgetCommand(2026, 3, BudgetSettings()))
        }

        assertEquals(ErrorCode.CONFLICT, exception.errorCode)
        assertEquals("A budget for 2026-03 already exists", exception.message)
    }

    @Test
    fun should_reject_invalid_period_or_settings_without_touching_repository() {
        mapOf(
            CreateBudgetCommand(1999, 13, BudgetSettings()) to listOf("year", "month"),
            CreateBudgetCommand(2026, 1, BudgetSettings(BigDecimal.ZERO, -1)) to listOf("exchangeRate", "workDays"),
            CreateBudgetCommand(2026, 1, BudgetSettings(BigDecimal("-1"), 32)) to listOf("exchangeRate", "workDays"),
            CreateBudgetCommand(2026, 1, BudgetSettings(BigDecimal("3300.001"))) to listOf("exchangeRate"),
        ).forEach { (command, fields) ->
            val exception = assertThrows<ValidationException> { useCase.create(userId, command) }
            assertEquals(fields, exception.violations.map { it.field })
        }
        assertThrows<ValidationException> { useCase.update(userId, UUID.randomUUID(), BudgetSettings(null, 40)) }
        verify(exactly = 0) { writer.insertIfAbsent(any()) }
        verify(exactly = 0) { reader.findById(any(), any()) }
    }

    @Test
    fun should_reject_totals_that_do_not_fit_the_money_format() {
        income = BigDecimal("10000000000000000.00")
        amounts[BudgetComponent.SAVINGS] = BigDecimal("10000000000000000.00")

        val exception = assertThrows<ValidationException> {
            useCase.create(userId, CreateBudgetCommand(2026, 1, BudgetSettings()))
        }

        assertEquals(listOf("totalIncome", "components.SAVINGS"), exception.violations.map { it.field })
        verify(exactly = 0) { writer.insertIfAbsent(any()) }
    }

    @Test
    fun should_list_newest_month_first_and_pass_validated_year_filter() {
        val unsorted = listOf(budget(2025, 12), budget(2026, 2), budget(2026, 11), budget(2024, 1))
        every { reader.findAll(userId, null) } returns unsorted
        every { reader.findAll(userId, 2026) } returns listOf(budget(2026, 1))

        val periods = useCase.list(userId, null).map { it.period.toString() }

        assertEquals(listOf("2026-11", "2026-02", "2025-12", "2024-01"), periods)
        assertEquals(1, useCase.list(userId, 2026).size)
        assertThrows<ValidationException> { useCase.list(userId, 1999) }
    }

    @Test
    fun should_get_budget_of_month_or_throw_not_found() {
        val budget = budget(2026, 10)
        every { reader.findByPeriod(userId, BudgetPeriod(2026, 10)) } returns budget
        every { reader.findByPeriod(userId, BudgetPeriod(2026, 9)) } returns null

        assertEquals(budget, useCase.get(userId, 2026, 10))
        val exception = assertThrows<ResourceNotFoundException> { useCase.get(userId, 2026, 9) }
        assertEquals("Monthly budget not found", exception.message)
        assertThrows<ValidationException> { useCase.get(userId, 2026, 13) }
    }

    @Test
    fun should_replace_settings_and_recalculate_when_updating() {
        val current = budget(2026, 10).copy(settings = BudgetSettings(BigDecimal("4000"), 20))
        every { reader.findById(userId, current.id) } returns current
        every { writer.update(any()) } answers { firstArg() }

        val updated = useCase.update(userId, current.id, BudgetSettings(null, 21))

        assertEquals(
            current.copy(settings = BudgetSettings(null, 21), figures = updated.figures, updatedAt = NOW),
            updated,
        )
        assertEquals("200.00", updated.figures.totalExpenses.toPlainString())
        assertEquals(NOW, updated.updatedAt)
    }

    @Test
    fun should_recalculate_from_current_records_and_keep_settings() {
        val current = budget(2026, 10).copy(settings = BudgetSettings(BigDecimal("4000"), 20))
        every { reader.findById(userId, current.id) } returns current
        every { writer.update(any()) } answers { firstArg() }
        amounts[BudgetComponent.CREDIT_CARDS] = BigDecimal("300.00")

        val recalculated = useCase.recalculate(userId, current.id)

        assertEquals(current.settings, recalculated.settings)
        assertEquals("500.00", recalculated.figures.totalExpenses.toPlainString())
        assertEquals("500.00", recalculated.figures.monthlyCashFlow.toPlainString())
        verify(exactly = 1) { writer.update(recalculated) }
    }

    @Test
    fun should_throw_not_found_when_budget_is_missing_or_of_another_user() {
        val id = UUID.randomUUID()
        every { reader.findById(userId, id) } returns null

        assertThrows<ResourceNotFoundException> { useCase.recalculate(userId, id) }
        assertThrows<ResourceNotFoundException> { useCase.update(userId, id, BudgetSettings()) }
        verify(exactly = 0) { writer.update(any()) }
    }

    @Test
    fun should_return_stored_budget_or_throw_not_found_when_writer_reports_no_row() {
        val current = budget(2026, 10)
        val stored = Instant.parse("2026-10-21T14:30:00.123456Z")
        every { reader.findById(userId, current.id) } returns current
        every { writer.update(any()) } answers { firstArg<MonthlyBudget>().copy(updatedAt = stored) } andThen null

        assertEquals(stored, useCase.recalculate(userId, current.id).updatedAt)
        assertThrows<ResourceNotFoundException> { useCase.recalculate(userId, current.id) }
    }

    @Test
    fun should_require_exactly_one_strategy_per_component() {
        val missing = BudgetComponent.entries.drop(1).map(::strategy)
        val duplicated = BudgetComponent.entries.map(::strategy) + strategy(BudgetComponent.LOANS)

        assertThrows<IllegalArgumentException> { useCaseWith(missing) }
        assertThrows<IllegalArgumentException> { useCaseWith(duplicated) }
    }

    private fun useCaseWith(strategies: List<BudgetComponentStrategy>) = MonthlyBudgetUseCaseImpl(
        reader,
        writer,
        sourceTotals,
        strategies,
        BudgetCalculationService(),
        Clock.fixed(NOW, ZoneOffset.UTC),
    )

    private fun strategy(component: BudgetComponent) = object : BudgetComponentStrategy {
        override val component = component

        override fun monthlyAmount(sources: BudgetSourceAmounts): BigDecimal = amounts[component] ?: BigDecimal.ZERO
    }

    private fun sourcesWith(totalIncome: BigDecimal) = BigDecimal.ZERO.let {
        BudgetSourceAmounts(totalIncome, it, it, it, it, it, it)
    }

    private fun budget(year: Int, month: Int) = MonthlyBudget(
        id = UUID.randomUUID(),
        userId = userId,
        period = BudgetPeriod(year, month),
        settings = BudgetSettings(),
        figures = BudgetCalculationService().calculate(BigDecimal.ZERO, emptyMap()),
        createdAt = EARLIER,
        updatedAt = EARLIER,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-21T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
