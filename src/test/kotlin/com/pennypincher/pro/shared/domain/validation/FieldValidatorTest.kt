package com.pennypincher.pro.shared.domain.validation

import com.pennypincher.pro.shared.domain.exception.FieldViolation
import com.pennypincher.pro.shared.domain.exception.ValidationException
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class FieldValidatorTest {
    @Test
    fun should_pass_when_every_rule_holds() {
        assertDoesNotThrow {
            FieldValidator()
                .requireText("name", "  ${"x".repeat(MAX)}  ", MAX)
                .requireNonNegativeAmount("value", BigDecimal.ZERO)
                .requireNonNegativeAmount("other", BigDecimal("12.34"))
                .validate()
        }
    }

    @Test
    fun should_report_every_broken_rule_together_when_several_fields_are_invalid() {
        val exception = assertThrows<ValidationException> {
            FieldValidator()
                .requireText("name", "   ", MAX)
                .requireNonNegativeAmount("value", BigDecimal("-0.01"))
                .requireNonNegativeAmount("balance", BigDecimal("1.001"))
                .validate()
        }

        assertEquals(
            listOf(
                FieldViolation("name", "must be between 1 and $MAX characters"),
                FieldViolation("value", "must be greater than or equal to 0"),
                FieldViolation("balance", "must have at most 16 integer digits and 2 decimal places"),
            ),
            exception.violations,
        )
    }

    @Test
    fun should_reject_text_when_trimmed_length_exceeds_maximum() {
        val exception = assertThrows<ValidationException> {
            FieldValidator().requireText("name", "x".repeat(MAX + 1), MAX).validate()
        }

        assertEquals("name", exception.violations.single().field)
    }

    @Test
    fun should_record_message_only_when_condition_is_false() {
        assertDoesNotThrow { FieldValidator().require(true, "date", "is wrong").validate() }

        val exception =
            assertThrows<ValidationException> { FieldValidator().require(false, "date", "is wrong").validate() }

        assertEquals(listOf(FieldViolation("date", "is wrong")), exception.violations)
    }

    private companion object {
        const val MAX = 5
    }
}
