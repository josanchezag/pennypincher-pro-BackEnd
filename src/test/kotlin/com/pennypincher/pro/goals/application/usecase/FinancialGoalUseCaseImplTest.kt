package com.pennypincher.pro.goals.application.usecase

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.FinancialGoalCommand
import com.pennypincher.pro.goals.domain.model.StandardGoals
import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import com.pennypincher.pro.goals.domain.port.outbound.FinancialGoalRepository
import com.pennypincher.pro.goals.domain.service.GoalProjectionService
import com.pennypincher.pro.networth.domain.model.NetWorthSummary
import com.pennypincher.pro.networth.domain.port.inbound.NetWorthUseCase
import com.pennypincher.pro.shared.domain.exception.ResourceNotFoundException
import com.pennypincher.pro.shared.domain.exception.ValidationException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class FinancialGoalUseCaseImplTest {
    private val repository = mockk<FinancialGoalRepository>()
    private val netWorthUseCase = mockk<NetWorthUseCase>()
    private val useCase = FinancialGoalUseCaseImpl(
        repository,
        netWorthUseCase,
        GoalProjectionService(),
        Clock.fixed(NOW, ZoneOffset.UTC),
    )
    private val userId = UUID.randomUUID()

    @Test
    fun should_save_trimmed_goal_with_scaled_amounts_when_creating() {
        every { repository.insert(any()) } answers { firstArg() }

        val created = useCase.create(userId, command(name = " Seguridad Financiera "))

        assertEquals("Seguridad Financiera", created.name)
        assertEquals(userId, created.userId)
        assertEquals(GoalLevel.FINANCIAL_SECURITY, created.level)
        assertEquals("605000000.00", created.targetAmount.toPlainString())
        assertEquals("1470000.00", created.monthlyInvestment.toPlainString())
        assertEquals("0.1100", created.expectedAnnualReturn.toPlainString())
        assertEquals(TARGET_YEARS, created.targetYears)
        assertEquals(NOW, created.createdAt)
        assertEquals(NOW, created.updatedAt)
    }

    @Test
    fun should_replace_every_field_keep_identity_and_clear_target_years_when_updating_without_them() {
        val current = existingGoal()
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.update(any()) } answers { firstArg() }
        val command = FinancialGoalCommand(
            "Libertad",
            GoalLevel.FINANCIAL_FREEDOM,
            BigDecimal("2066000000"),
            BigDecimal("7000000.5"),
            BigDecimal("0.115"),
        )

        val updated = useCase.update(userId, current.id, command)

        assertEquals(current.id, updated.id)
        assertEquals(EARLIER, updated.createdAt)
        assertEquals(NOW, updated.updatedAt)
        assertEquals(GoalLevel.FINANCIAL_FREEDOM, updated.level)
        assertEquals("7000000.50", updated.monthlyInvestment.toPlainString())
        assertEquals("0.1150", updated.expectedAnnualReturn.toPlainString())
        assertNull(updated.targetYears)
        verify(exactly = 1) { repository.update(updated) }
    }

    @Test
    fun should_report_not_found_when_goal_is_deleted_between_reading_and_updating_it() {
        val current = existingGoal()
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.update(any()) } returns null

        val exception = assertThrows<ResourceNotFoundException> { useCase.update(userId, current.id, command()) }

        assertEquals("Financial goal not found", exception.message)
        verify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun should_order_by_level_then_target_then_name_ignoring_case_when_listing() {
        val goals = listOf(
            existingGoal("b", GoalLevel.ABSOLUTE_FREEDOM, "1"),
            existingGoal("z", GoalLevel.FINANCIAL_SECURITY, "200"),
            existingGoal("B", GoalLevel.FINANCIAL_SECURITY, "100"),
            existingGoal("a", GoalLevel.FINANCIAL_SECURITY, "100"),
            existingGoal("c", GoalLevel.FINANCIAL_INDEPENDENCE, "1"),
        )
        every { repository.findAllActive(userId, null) } returns goals

        assertEquals(listOf("a", "B", "z", "c", "b"), useCase.list(userId, null).map { it.name })
    }

    @Test
    fun should_accept_boundary_values_when_validating() {
        every { repository.insert(any()) } answers { firstArg() }

        assertDoesNotThrow {
            useCase.create(userId, command(target = "0.01", monthly = "0", annualReturn = "1", years = 1))
            useCase.create(userId, command(annualReturn = "0.0001", years = MAX_YEARS))
            useCase.create(userId, command(annualReturn = "0.11000", years = null))
        }
    }

    @Test
    fun should_report_every_invalid_field_without_saving_when_command_breaks_the_rules() {
        val exception = assertThrows<ValidationException> {
            useCase.create(userId, command(name = " ", target = "0", monthly = "-1", annualReturn = "0", years = 0))
        }

        assertEquals(
            listOf("name", "targetAmount", "monthlyInvestment", "expectedAnnualReturn", "targetYears"),
            exception.violations.map { it.field },
        )
        verify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun should_reject_each_out_of_range_or_too_precise_value_when_validating() {
        val invalid = listOf(
            command(target = "-5") to "targetAmount",
            command(target = "1.001") to "targetAmount",
            command(target = "12345678901234567") to "targetAmount",
            command(monthly = "0.001") to "monthlyInvestment",
            command(annualReturn = "-0.1") to "expectedAnnualReturn",
            command(annualReturn = "1.0001") to "expectedAnnualReturn",
            command(annualReturn = "0.11111") to "expectedAnnualReturn",
            command(years = MAX_YEARS + 1) to "targetYears",
            command(name = "x".repeat(NAME_TOO_LONG)) to "name",
        )

        invalid.forEach { (command, field) ->
            val exception = assertThrows<ValidationException> { useCase.create(userId, command) }
            assertEquals(listOf(field), exception.violations.map { it.field })
        }
    }

    @Test
    fun should_use_specific_messages_for_annual_return_range_and_precision() {
        val range = assertThrows<ValidationException> { useCase.create(userId, command(annualReturn = "2")) }
        val precision = assertThrows<ValidationException> { useCase.create(userId, command(annualReturn = "0.12345")) }

        assertEquals("must be greater than 0 and at most 1", range.violations.single().message)
        assertEquals("must have at most 4 decimal places", precision.violations.single().message)
    }

    @Test
    fun should_create_standard_goals_only_for_levels_without_goal_when_creating_defaults() {
        val existing = existingGoal("Mi seguridad", GoalLevel.FINANCIAL_SECURITY, "1")
        val saved = mutableListOf<FinancialGoal>()
        every { repository.findAllActive(userId, null) } answers { listOf(existing) + saved }
        every { repository.insert(capture(saved)) } answers { firstArg() }

        val goals = useCase.createDefaults(userId)

        assertEquals(
            listOf("Mi seguridad", "Independencia Financiera", "Libertad Financiera", "Libertad Absoluta"),
            goals.map { it.name },
        )
        assertEquals(StandardGoals.ALL.drop(1).map { it.targetYears }, saved.map { it.targetYears })
        assertEquals("4366000000.00", saved.last().targetAmount.toPlainString())
        assertEquals("15850000.00", saved.last().monthlyInvestment.toPlainString())
    }

    @Test
    fun should_create_nothing_when_every_level_already_has_a_goal() {
        val goals = GoalLevel.entries.map { existingGoal(it.name, it, "1") }
        every { repository.findAllActive(userId, null) } returns goals

        assertEquals(goals, useCase.createDefaults(userId))
        verify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun should_hard_delete_through_repository_and_report_missing_goal_as_not_found() {
        val id = UUID.randomUUID()
        every { repository.delete(userId, id) } returns true
        useCase.delete(userId, id)
        verify(exactly = 1) { repository.delete(userId, id) }

        every { repository.delete(userId, id) } returns false
        val exception = assertThrows<ResourceNotFoundException> { useCase.delete(userId, id) }
        assertEquals("Financial goal not found", exception.message)
    }

    @Test
    fun should_describe_each_level_in_display_order() {
        assertEquals(
            listOf(
                "Expenses covered for 6 months without income",
                "Passive income covers all basic expenses",
                "Passive income covers desired lifestyle",
                "Unlimited financial choices",
            ),
            GoalLevel.entries.map { it.description },
        )
    }

    @Test
    fun should_project_goal_against_live_net_worth_and_current_month_when_getting_projection() {
        val goal = existingGoal()
        every { repository.findActiveById(userId, goal.id) } returns goal
        every { netWorthUseCase.getCurrentNetWorth(userId) } returns
            NetWorthSummary(BigDecimal("500000000.00"), BigDecimal("76717227.00"))

        val projection = useCase.getProjection(userId, goal.id)

        assertEquals(goal, projection.goal)
        assertEquals("423282773.00", projection.currentNetWorth.toPlainString())
        assertEquals("69.96", projection.progressPercentage.toPlainString())
        assertEquals(LocalDate.parse("2026-10-01"), projection.dataPoints.first().date)
    }

    @Test
    fun should_report_not_found_without_reading_net_worth_when_projecting_a_missing_goal() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null

        assertThrows<ResourceNotFoundException> { useCase.getProjection(userId, id) }
        verify(exactly = 0) { netWorthUseCase.getCurrentNetWorth(any()) }
    }

    @Test
    fun should_project_every_goal_in_list_order_reading_net_worth_once() {
        val goals = listOf(
            existingGoal("libre", GoalLevel.FINANCIAL_FREEDOM, "2066000000"),
            existingGoal("segura", GoalLevel.FINANCIAL_SECURITY, "605000000"),
        )
        every { repository.findAllActive(userId, null) } returns goals
        every { netWorthUseCase.getCurrentNetWorth(userId) } returns
            NetWorthSummary(BigDecimal("1.00"), BigDecimal.ZERO)

        val projections = useCase.getAllProjections(userId)

        assertEquals(listOf("segura", "libre"), projections.map { it.goal.name })
        verify(exactly = 1) { netWorthUseCase.getCurrentNetWorth(userId) }
    }

    @Test
    fun should_return_no_projections_without_reading_net_worth_when_user_has_no_goals() {
        every { repository.findAllActive(userId, null) } returns emptyList()

        assertEquals(emptyList<Any>(), useCase.getAllProjections(userId))
        verify(exactly = 0) { netWorthUseCase.getCurrentNetWorth(any()) }
    }

    private fun command(
        name: String = "Seguridad Financiera",
        target: String = "605000000",
        monthly: String = "1470000",
        annualReturn: String = "0.11",
        years: Int? = TARGET_YEARS,
    ) = FinancialGoalCommand(
        name,
        GoalLevel.FINANCIAL_SECURITY,
        BigDecimal(target),
        BigDecimal(monthly),
        BigDecimal(annualReturn),
        years,
    )

    private fun existingGoal(
        name: String = "Seguridad",
        level: GoalLevel = GoalLevel.FINANCIAL_SECURITY,
        target: String = "605000000",
    ) = FinancialGoal(
        id = UUID.randomUUID(),
        userId = userId,
        name = name,
        level = level,
        targetAmount = BigDecimal(target),
        monthlyInvestment = BigDecimal("1470000.00"),
        expectedAnnualReturn = BigDecimal("0.1100"),
        targetYears = TARGET_YEARS,
        createdAt = EARLIER,
        updatedAt = EARLIER,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-23T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
        const val TARGET_YEARS = 16
        const val MAX_YEARS = 100
        const val NAME_TOO_LONG = 151
    }
}
