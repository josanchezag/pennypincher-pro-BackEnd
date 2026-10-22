package com.pennypincher.pro.shared.domain.model

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoneyTest {
    @Test
    fun should_fit_when_amount_has_at_most_16_integer_digits_and_2_decimals() {
        listOf("0", "0.5", "1500000", "1500000.10", "9999999999999999.99", "1E+3", "12.300").forEach {
            assertTrue(Money.fits(BigDecimal(it))) { it }
        }
    }

    @Test
    fun should_not_fit_when_amount_has_more_decimals_or_integer_digits() {
        listOf("0.001", "1.234", "10000000000000000", "12345678901234567.5").forEach {
            assertFalse(Money.fits(BigDecimal(it))) { it }
        }
    }

    @Test
    fun should_use_scale_two_when_normalizing_amounts() {
        assertEquals("1500000.00", Money.of(BigDecimal("1500000")).toPlainString())
        assertEquals("12.30", Money.of(BigDecimal("12.300")).toPlainString())
        assertEquals("0.00", Money.ZERO.toPlainString())
    }
}
