package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.Loan
import com.pennypincher.pro.expenses.domain.model.LoanCommand
import com.pennypincher.pro.expenses.domain.port.outbound.LoanRepository
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoanUseCaseImplTest {
    private val repository = mockk<LoanRepository>()
    private val useCase = LoanUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC))
    private val userId = UUID.randomUUID()

    @Test
    fun should_save_loan_with_rounded_original_amount_below_balance_when_creating() {
        every { repository.save(any()) } answers { firstArg() }

        val created = useCase.create(userId, command(originalAmount = BigDecimal("50000000.5")))

        assertEquals("Prestamo Occidente", created.name)
        assertEquals("1258908.00", created.monthlyPayment.toPlainString())
        assertEquals("88614841.00", created.totalBalance.toPlainString())
        assertEquals("50000000.50", created.originalAmount?.toPlainString())
        assertEquals(NOW, created.createdAt)
    }

    @Test
    fun should_leave_original_amount_untracked_when_creating_without_it() {
        every { repository.save(any()) } answers { firstArg() }

        assertNull(useCase.create(userId, command()).originalAmount)
    }

    @Test
    fun should_clear_original_amount_when_updating_without_it() {
        val current = existingLoan()
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.save(any()) } answers { firstArg() }

        val updated = useCase.update(userId, current.id, command(name = " Davivienda 7141 "))

        assertEquals(current.id, updated.id)
        assertEquals("Davivienda 7141", updated.name)
        assertNull(updated.originalAmount)
        assertEquals(NOW, updated.updatedAt)
        verify(exactly = 1) { repository.save(updated) }
    }

    @Test
    fun should_reject_without_touching_repository_when_original_amount_is_invalid() {
        listOf(BigDecimal("-0.01"), BigDecimal("1.001"), BigDecimal("12345678901234567")).forEach { amount ->
            val exception = assertThrows<ValidationException> {
                useCase.create(userId, command(originalAmount = amount))
            }
            assertEquals(listOf("originalAmount"), exception.violations.map { it.field })
        }
        val exception = assertThrows<ValidationException> {
            useCase.create(userId, LoanCommand(" ", BigDecimal("-1"), BigDecimal("-1"), BigDecimal("-1")))
        }
        assertEquals(
            listOf("name", "monthlyPayment", "totalBalance", "originalAmount"),
            exception.violations.map { it.field },
        )
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun should_throw_not_found_with_resource_name_when_loan_is_missing() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null

        val exception = assertThrows<ResourceNotFoundException> { useCase.get(userId, id) }

        assertEquals("Loan not found", exception.message)
    }

    private fun command(name: String = "Prestamo Occidente", originalAmount: BigDecimal? = null) =
        LoanCommand(name, BigDecimal("1258908"), BigDecimal("88614841"), originalAmount)

    private fun existingLoan() = Loan(
        id = UUID.randomUUID(),
        userId = userId,
        name = "Davivienda",
        monthlyPayment = BigDecimal("1339823.00"),
        totalBalance = BigDecimal("59781329.00"),
        originalAmount = BigDecimal("90000000.00"),
        createdAt = EARLIER,
        updatedAt = EARLIER,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-22T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
