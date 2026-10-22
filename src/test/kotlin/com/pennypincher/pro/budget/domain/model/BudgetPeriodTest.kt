package com.pennypincher.pro.budget.domain.model

import com.pennypincher.pro.shared.domain.exception.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class BudgetPeriodTest {
    @Test
    fun should_accept_the_bounds_of_the_year_and_month_ranges() {
        assertEquals(BudgetPeriod(2000, 1), BudgetPeriod.of(2000, 1))
        assertEquals(BudgetPeriod(2100, 12), BudgetPeriod.of(2100, 12))
        assertDoesNotThrow { BudgetPeriod.requireValidYear(2026) }
    }

    @Test
    fun should_reject_with_every_broken_field_when_year_or_month_is_out_of_range() {
        mapOf(
            (1999 to 5) to listOf("year"),
            (2101 to 5) to listOf("year"),
            (2026 to 0) to listOf("month"),
            (2026 to 13) to listOf("month"),
            (0 to 0) to listOf("year", "month"),
        ).forEach { (period, fields) ->
            val exception = assertThrows<ValidationException> { BudgetPeriod.of(period.first, period.second) }
            assertEquals(fields, exception.violations.map { it.field })
        }
        assertThrows<ValidationException> { BudgetPeriod.requireValidYear(1999) }
    }

    @Test
    fun should_format_as_year_and_two_digit_month() {
        assertEquals("2026-03", BudgetPeriod.of(2026, 3).toString())
        assertEquals("2026-10", BudgetPeriod.of(2026, 10).toString())
    }
}
