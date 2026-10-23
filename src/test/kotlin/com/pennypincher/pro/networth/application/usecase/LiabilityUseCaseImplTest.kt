package com.pennypincher.pro.networth.application.usecase

import com.pennypincher.pro.networth.domain.model.Liability
import com.pennypincher.pro.networth.domain.model.LiabilityCommand
import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import com.pennypincher.pro.networth.domain.port.outbound.LiabilityRepository
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

class LiabilityUseCaseImplTest {
    private val repository = mockk<LiabilityRepository>()
    private val useCase = LiabilityUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC))
    private val userId = UUID.randomUUID()

    @Test
    fun should_sort_by_category_then_name_ignoring_case_when_listing() {
        val unsorted = listOf(
            liability("Tío", LiabilityCategory.FAMILY_DEBT),
            liability("visa", LiabilityCategory.CREDIT_CARD),
            liability("Davivienda 7141", LiabilityCategory.BANK_LOAN),
            liability("Nu Bank", LiabilityCategory.CREDIT_CARD),
        )
        every { repository.findAllActive(userId, null) } returns unsorted

        val names = useCase.list(userId, null).map { it.name }

        assertEquals(listOf("Nu Bank", "visa", "Davivienda 7141", "Tío"), names)
    }

    @Test
    fun should_return_total_balance_with_two_decimals() {
        every { repository.sumActive(userId) } returns BigDecimal("1250000.5")

        assertEquals("1250000.50", useCase.total(userId).toPlainString())
    }

    @Test
    fun should_save_trimmed_liability_with_scaled_balance_when_creating() {
        every { repository.insert(any()) } answers { firstArg() }

        val created = useCase.create(userId, LiabilityCommand(" Nu Bank ", BigDecimal("2500000"), CREDIT_CARD))

        assertEquals(userId, created.userId)
        assertEquals("Nu Bank", created.name)
        assertEquals("2500000.00", created.currentBalance.toPlainString())
        assertEquals(CREDIT_CARD, created.category)
        assertEquals(NOW, created.createdAt)
        assertEquals(NOW, created.updatedAt)
    }

    @Test
    fun should_report_current_balance_field_when_balance_is_negative_or_too_precise() {
        listOf("-1", "0.001").forEach { balance ->
            val exception = assertThrows<ValidationException> {
                useCase.create(userId, LiabilityCommand("Nu Bank", BigDecimal(balance), CREDIT_CARD))
            }
            assertEquals(listOf("currentBalance"), exception.violations.map { it.field })
        }
        val blankName = assertThrows<ValidationException> {
            useCase.create(userId, LiabilityCommand(" ", BigDecimal.ZERO, CREDIT_CARD))
        }
        assertEquals(listOf("name"), blankName.violations.map { it.field })
        verify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun should_apply_changes_and_keep_creation_time_when_updating() {
        val current = liability("Visa", CREDIT_CARD)
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.update(any()) } answers { firstArg() }

        val command = LiabilityCommand("Préstamo Davivienda", BigDecimal("9000000.1"), LiabilityCategory.BANK_LOAN)
        val updated = useCase.update(userId, current.id, command)

        assertEquals(current.id, updated.id)
        assertEquals(current.createdAt, updated.createdAt)
        assertEquals("Préstamo Davivienda", updated.name)
        assertEquals("9000000.10", updated.currentBalance.toPlainString())
        assertEquals(LiabilityCategory.BANK_LOAN, updated.category)
        assertEquals(NOW, updated.updatedAt)
    }

    @Test
    fun should_throw_not_found_when_liability_is_missing_or_of_another_user() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null
        every { repository.delete(userId, id) } returns false

        val exception = assertThrows<ResourceNotFoundException> {
            useCase.update(userId, id, LiabilityCommand("Visa", BigDecimal.ONE, CREDIT_CARD))
        }
        assertEquals("Liability not found", exception.message)
        assertThrows<ResourceNotFoundException> { useCase.delete(userId, id) }
    }

    private fun liability(name: String, category: LiabilityCategory) = Liability(
        id = UUID.randomUUID(),
        userId = userId,
        name = name,
        currentBalance = BigDecimal("100.00"),
        category = category,
        createdAt = EARLIER,
        updatedAt = EARLIER,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-21T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
        val CREDIT_CARD = LiabilityCategory.CREDIT_CARD
    }
}
