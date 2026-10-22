package com.pennypincher.pro.expenses.application.usecase

import com.pennypincher.pro.expenses.domain.model.BalanceTotals
import com.pennypincher.pro.expenses.domain.model.CreditCard
import com.pennypincher.pro.expenses.domain.model.CreditCardCommand
import com.pennypincher.pro.expenses.domain.port.outbound.CreditCardRepository
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

class CreditCardUseCaseImplTest {
    private val repository = mockk<CreditCardRepository>()
    private val useCase = CreditCardUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC))
    private val userId = UUID.randomUUID()

    @Test
    fun should_sort_by_name_ignoring_case_when_listing() {
        every { repository.findAllActive(userId, null) } returns
            listOf(card("Tarjeta Nu Bank"), card("addi Yasmin"), card("Addi Josi"), card("Banco de Occidente"))

        val names = useCase.list(userId, null).map { it.name }

        assertEquals(listOf("Addi Josi", "addi Yasmin", "Banco de Occidente", "Tarjeta Nu Bank"), names)
    }

    @Test
    fun should_round_amounts_to_two_decimals_and_keep_count_when_summarizing() {
        every { repository.summarizeActive(userId) } returns BalanceTotals(BigDecimal.ZERO, BigDecimal("42956922"), 3)

        val summary = useCase.summary(userId)

        assertEquals("0.00", summary.totalMonthlyAmount.toPlainString())
        assertEquals("42956922.00", summary.totalBalance.toPlainString())
        assertEquals(3, summary.count)
    }

    @Test
    fun should_save_trimmed_card_with_two_decimal_amounts_when_creating() {
        every { repository.save(any()) } answers { firstArg() }

        val created = useCase.create(
            userId,
            CreditCardCommand(" Tarjeta Nu Bank ", BigDecimal.ZERO, BigDecimal("12407700")),
        )

        assertEquals(userId, created.userId)
        assertEquals("Tarjeta Nu Bank", created.name)
        assertEquals("0.00", created.monthlyPayment.toPlainString())
        assertEquals("12407700.00", created.totalBalance.toPlainString())
        assertEquals(NOW, created.createdAt)
        assertEquals(NOW, created.updatedAt)
    }

    @Test
    fun should_reject_command_without_touching_repository_when_a_field_is_invalid() {
        val tooLong = "x".repeat(RecordName.MAX_LENGTH + 1)
        val invalid = listOf(
            CreditCardCommand(" ", BigDecimal.ONE, BigDecimal.ONE) to listOf("name"),
            CreditCardCommand(tooLong, BigDecimal.ONE, BigDecimal.ONE) to listOf("name"),
            CreditCardCommand("Addi", BigDecimal("-0.01"), BigDecimal.ONE) to listOf("monthlyPayment"),
            CreditCardCommand("Addi", BigDecimal.ONE, BigDecimal("1.001")) to listOf("totalBalance"),
            CreditCardCommand("", BigDecimal("-1"), BigDecimal("12345678901234567")) to
                listOf("name", "monthlyPayment", "totalBalance"),
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
        val current = card("Addi")
        every { repository.findActiveById(userId, current.id) } returns current
        every { repository.save(any()) } answers { firstArg() }

        val updated = useCase.update(
            userId,
            current.id,
            CreditCardCommand(" Addi Josi ", BigDecimal.TEN, BigDecimal("5.5")),
        )

        assertEquals(current.id, updated.id)
        assertEquals(current.createdAt, updated.createdAt)
        assertEquals("Addi Josi", updated.name)
        assertEquals("10.00", updated.monthlyPayment.toPlainString())
        assertEquals("5.50", updated.totalBalance.toPlainString())
        assertEquals(NOW, updated.updatedAt)
        verify(exactly = 1) { repository.save(updated) }
    }

    @Test
    fun should_throw_not_found_with_resource_name_when_card_is_missing_or_of_another_user() {
        val id = UUID.randomUUID()
        every { repository.findActiveById(userId, id) } returns null
        every { repository.deactivate(userId, id) } returns false

        val exception = assertThrows<ResourceNotFoundException> { useCase.get(userId, id) }

        assertEquals("Credit card not found", exception.message)
        assertThrows<ResourceNotFoundException> { useCase.delete(userId, id) }
    }

    private fun card(name: String) = CreditCard(
        id = UUID.randomUUID(),
        userId = userId,
        name = name,
        monthlyPayment = BigDecimal("0.00"),
        totalBalance = BigDecimal("100.00"),
        createdAt = EARLIER,
        updatedAt = EARLIER,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-22T14:30:00Z")
        val EARLIER: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
