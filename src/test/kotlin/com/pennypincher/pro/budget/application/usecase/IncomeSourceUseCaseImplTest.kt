package com.pennypincher.pro.budget.application.usecase

import com.pennypincher.pro.budget.domain.model.IncomeSource
import com.pennypincher.pro.budget.domain.model.IncomeSourceCommand
import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import com.pennypincher.pro.budget.domain.port.outbound.IncomeSourceRepository
import com.pennypincher.pro.shared.domain.exception.ResourceNotFoundException
import com.pennypincher.pro.shared.domain.exception.ValidationException
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

class IncomeSourceUseCaseImplTest {
    private val repository = mockk<IncomeSourceRepository>()
    private val useCase = IncomeSourceUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC))
    private val userId = UUID.randomUUID()

    @Test
    fun should_sort_by_category_then_name_ignoring_case_when_listing() {
        every { repository.findAllActive(userId, null) } returns listOf(
            source("Suasie", IncomeCategory.BUSINESS),
            source("remoti", IncomeCategory.EMPLOYMENT),
            source("Arriendo Madera", IncomeCategory.RENTAL),
            source("AvenueCode", IncomeCategory.EMPLOYMENT),
            source("Otro", IncomeCategory.OTHER),
        )

        val names = useCase.list(userId, null).map { it.name }

        assertEquals(listOf("AvenueCode", "remoti", "Arriendo Madera", "Suasie", "Otro"), names)
    }

    @Test
    fun should_return_total_monthly_income_with_two_decimals() {
        every { repository.sumActive(userId) } returns BigDecimal("37152922.36")

        assertEquals("37152922.36", useCase.total(userId).toPlainString())
    }

    @Test
    fun should_save_trimmed_source_with_default_currency_when_creating_without_currency() {
        every { repository.save(any()) } answers { firstArg() }

        val created = useCase.create(userId, IncomeSourceCommand(" AvenueCode ", EMPLOYMENT, BigDecimal("15878607.3")))

        assertEquals(userId, created.userId)
        assertEquals("AvenueCode", created.name)
        assertEquals(EMPLOYMENT, created.category)
        assertEquals("15878607.30", created.amount.toPlainString())
        assertEquals(IncomeSource.DEFAULT_CURRENCY, created.currency)
        assertEquals(NOW, created.createdAt)
        assertEquals(NOW, created.updatedAt)
    }

    @Test
    fun should_keep_given_currency_when_creating() {
        every { repository.save(any()) } answers { firstArg() }

        val created = useCase.create(userId, IncomeSourceCommand("Remoti", EMPLOYMENT, BigDecimal.ONE, "USD"))

        assertEquals("USD", created.currency)
    }

    @Test
    fun should_reject_command_without_touching_repository_when_a_field_is_invalid() {
        val invalid = listOf(
            IncomeSourceCommand(" ", EMPLOYMENT, BigDecimal.ONE) to listOf("name"),
            IncomeSourceCommand("x".repeat(IncomeSource.MAX_NAME_LENGTH + 1), EMPLOYMENT, BigDecimal.ONE) to
                listOf("name"),
            IncomeSourceCommand("Licor", EMPLOYMENT, BigDecimal("-0.01")) to listOf("amount"),
            IncomeSourceCommand("Licor", EMPLOYMENT, BigDecimal("1.001")) to listOf("amount"),
            IncomeSourceCommand("Licor", EMPLOYMENT, BigDecimal.ONE, "usd") to listOf("currency"),
            IncomeSourceCommand("Licor", EMPLOYMENT, BigDecimal.ONE, "DOLLAR") to listOf("currency"),
            IncomeSourceCommand("", EMPLOYMENT, BigDecimal("-1"), "") to listOf("name", "amount", "currency"),
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
    fun should_replace_every_field_and_reset_currency_to_default_when_updating_without_currency() {
        val current = source("Airbnb", IncomeCategory.RENTAL).copy(currency = "USD")
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.save(any()) } answers { firstArg() }

        val updated = useCase.update(
            userId,
            current.id,
            IncomeSourceCommand(" Airbnb Barranca ", RENTAL, BigDecimal.TEN),
        )

        assertEquals(current.id, updated.id)
        assertEquals(current.createdAt, updated.createdAt)
        assertEquals("Airbnb Barranca", updated.name)
        assertEquals("10.00", updated.amount.toPlainString())
        assertEquals(IncomeSource.DEFAULT_CURRENCY, updated.currency)
        assertEquals(NOW, updated.updatedAt)
        verify(exactly = 1) { repository.save(updated) }
    }

    @Test
    fun should_throw_not_found_with_resource_name_when_source_is_missing_or_of_another_user() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null
        every { repository.deactivate(userId, id) } returns false

        val exception = assertThrows<ResourceNotFoundException> { useCase.get(userId, id) }

        assertEquals("Income source not found", exception.message)
        assertThrows<ResourceNotFoundException> { useCase.delete(userId, id) }
    }

    private fun source(name: String, category: IncomeCategory) = IncomeSource(
        id = UUID.randomUUID(),
        userId = userId,
        name = name,
        category = category,
        amount = BigDecimal("100.00"),
        currency = IncomeSource.DEFAULT_CURRENCY,
        createdAt = EARLIER,
        updatedAt = EARLIER,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-21T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
        val EMPLOYMENT = IncomeCategory.EMPLOYMENT
        val RENTAL = IncomeCategory.RENTAL
    }
}
