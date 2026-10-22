package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.SavingsAccount
import com.pennypincher.pro.expenses.domain.model.SavingsAccountCommand
import com.pennypincher.pro.expenses.domain.port.outbound.SavingsAccountRepository
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

class SavingsAccountUseCaseImplTest {
    private val repository = mockk<SavingsAccountRepository>()
    private val useCase = SavingsAccountUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC))
    private val userId = UUID.randomUUID()

    @Test
    fun should_save_trimmed_account_with_two_decimal_amounts_when_creating() {
        every { repository.save(any()) } answers { firstArg() }

        val created = useCase.create(
            userId,
            SavingsAccountCommand(" Cash USD ", BigDecimal("250000.5"), BigDecimal("5082000")),
        )

        assertEquals("Cash USD", created.name)
        assertEquals("250000.50", created.monthlyContribution.toPlainString())
        assertEquals("5082000.00", created.currentBalance.toPlainString())
        assertEquals(NOW, created.createdAt)
    }

    @Test
    fun should_report_savings_field_names_when_amounts_are_invalid() {
        val exception = assertThrows<ValidationException> {
            useCase.create(userId, SavingsAccountCommand("Licor", BigDecimal("-1"), BigDecimal("0.001")))
        }

        assertEquals(listOf("monthlyContribution", "currentBalance"), exception.violations.map { it.field })
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun should_replace_every_field_and_keep_identity_when_updating() {
        val current =
            SavingsAccount(UUID.randomUUID(), userId, "Colchon", BigDecimal.ZERO, BigDecimal.ONE, EARLIER, EARLIER)
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.save(any()) } answers { firstArg() }

        val updated = useCase.update(
            userId,
            current.id,
            SavingsAccountCommand("Colchon Licor", BigDecimal.TEN, BigDecimal("56000000")),
        )

        assertEquals(current.id, updated.id)
        assertEquals("Colchon Licor", updated.name)
        assertEquals("10.00", updated.monthlyContribution.toPlainString())
        assertEquals("56000000.00", updated.currentBalance.toPlainString())
        assertEquals(EARLIER, updated.createdAt)
        assertEquals(NOW, updated.updatedAt)
    }

    @Test
    fun should_throw_not_found_with_resource_name_when_account_is_missing() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null

        val exception = assertThrows<ResourceNotFoundException> { useCase.get(userId, id) }

        assertEquals("Savings account not found", exception.message)
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-22T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
