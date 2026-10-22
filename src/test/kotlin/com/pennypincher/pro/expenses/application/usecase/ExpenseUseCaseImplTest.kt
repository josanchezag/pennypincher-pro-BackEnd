package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.CategoryTotal
import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.expenses.domain.model.ExpenseCommand
import com.pennypincher.pro.expenses.domain.model.enums.FixedExpenseCategory
import com.pennypincher.pro.expenses.domain.port.outbound.ExpenseRepository
import com.pennypincher.pro.shared.domain.exception.ResourceNotFoundException
import com.pennypincher.pro.shared.domain.exception.ValidationException
import com.pennypincher.pro.shared.domain.model.RecordName
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExpenseUseCaseImplTest {
    private val repository = mockk<ExpenseRepository<FixedExpenseCategory>>()
    private val useCase = ExpenseUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC), "Fixed expense")
    private val userId = UUID.randomUUID()

    @Test
    fun should_sort_by_category_then_name_ignoring_case_when_listing() {
        every { repository.findAllActive(userId, null) } returns listOf(
            expense("Natación Thiana", EDUCATION),
            expense("Otro", FixedExpenseCategory.OTHER),
            expense("colegio Thiana", EDUCATION),
            expense("Mercado", FixedExpenseCategory.FOOD),
        )

        val names = useCase.list(userId, null).map { it.name }

        assertEquals(listOf("Mercado", "colegio Thiana", "Natación Thiana", "Otro"), names)
    }

    @Test
    fun should_pass_category_filter_to_repository_when_listing() {
        every { repository.findAllActive(userId, EDUCATION) } returns listOf(expense("Colegio", EDUCATION))

        assertEquals(listOf("Colegio"), useCase.list(userId, EDUCATION).map { it.name })
    }

    @Test
    fun should_build_summary_from_category_totals_when_summarizing() {
        every { repository.totalsByCategory(userId) } returns
            listOf(CategoryTotal(EDUCATION, BigDecimal("2347709"), 2), CategoryTotal(CHARITY, BigDecimal("50000"), 1))

        val summary = useCase.summary(userId)

        assertEquals("2397709.00", summary.totalMonthlyAmount.toPlainString())
        assertEquals(3, summary.count)
        assertEquals(listOf(EDUCATION, CHARITY), summary.byCategory.map { it.category })
    }

    @Test
    fun should_save_trimmed_expense_with_two_decimal_amount_when_creating() {
        every { repository.save(any()) } answers { firstArg() }

        val created = useCase.create(userId, ExpenseCommand(" Donación Aldeas ", BigDecimal("50000"), CHARITY))

        assertEquals(userId, created.userId)
        assertEquals("Donación Aldeas", created.name)
        assertEquals("50000.00", created.monthlyAmount.toPlainString())
        assertEquals(CHARITY, created.category)
        assertEquals(NOW, created.createdAt)
        assertEquals(NOW, created.updatedAt)
    }

    @Test
    fun should_reject_command_without_touching_repository_when_a_field_is_invalid() {
        val invalid = listOf(
            ExpenseCommand(" ", BigDecimal.ONE, EDUCATION) to listOf("name"),
            ExpenseCommand("x".repeat(RecordName.MAX_LENGTH + 1), BigDecimal.ONE, EDUCATION) to listOf("name"),
            ExpenseCommand("SOAT", BigDecimal("-0.01"), EDUCATION) to listOf("monthlyAmount"),
            ExpenseCommand("", BigDecimal("1.001"), EDUCATION) to listOf("name", "monthlyAmount"),
        )

        invalid.forEach { (command, fields) ->
            val exception = assertThrows<ValidationException> { useCase.create(userId, command) }
            assertEquals(fields, exception.violations.map { it.field })
            assertThrows<ValidationException> { useCase.update(userId, UUID.randomUUID(), command) }
        }
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { repository.findActiveById(any(), any()) }
    }

    @Test
    fun should_replace_every_field_and_keep_identity_when_updating() {
        val current = expense("Poliza", FixedExpenseCategory.OTHER)
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.save(any()) } answers { firstArg() }

        val updated = useCase.update(
            userId,
            current.id,
            ExpenseCommand(" Poliza Salud ", BigDecimal("1382421"), FixedExpenseCategory.INSURANCE),
        )

        assertEquals(current.id, updated.id)
        assertEquals(current.createdAt, updated.createdAt)
        assertEquals("Poliza Salud", updated.name)
        assertEquals("1382421.00", updated.monthlyAmount.toPlainString())
        assertEquals(FixedExpenseCategory.INSURANCE, updated.category)
        assertEquals(NOW, updated.updatedAt)
        verify(exactly = 1) { repository.save(updated) }
    }

    @Test
    fun should_throw_not_found_with_resource_name_when_expense_is_missing() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null

        val exception = assertThrows<ResourceNotFoundException> { useCase.get(userId, id) }

        assertEquals("Fixed expense not found", exception.message)
    }

    private fun expense(name: String, category: FixedExpenseCategory) = Expense(
        id = UUID.randomUUID(),
        userId = userId,
        name = name,
        monthlyAmount = BigDecimal("100.00"),
        category = category,
        createdAt = EARLIER,
        updatedAt = EARLIER,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-22T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
        val EDUCATION = FixedExpenseCategory.EDUCATION
        val CHARITY = FixedExpenseCategory.CHARITY
    }
}
