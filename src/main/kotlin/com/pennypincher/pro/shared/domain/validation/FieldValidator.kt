package com.pennypincher.pro.shared.domain.validation

import com.pennypincher.pro.shared.domain.exception.FieldViolation
import com.pennypincher.pro.shared.domain.exception.ValidationException
import com.pennypincher.pro.shared.domain.model.Money
import java.math.BigDecimal

/** Collects every broken field rule and reports them together in one [ValidationException]. */
class FieldValidator {
    private val violations = mutableListOf<FieldViolation>()

    /** Records [message] for [field] when [condition] is false. */
    fun require(condition: Boolean, field: String, message: String): FieldValidator = apply {
        if (!condition) violations += FieldViolation(field, message)
    }

    /** The trimmed text must keep between 1 and [maxLength] characters. */
    fun requireText(field: String, value: String, maxLength: Int): FieldValidator = apply {
        if (value.isBlank() || value.trim().length > maxLength) {
            violations += FieldViolation(field, "must be between 1 and $maxLength characters")
        }
    }

    /** The amount must be zero or positive and fit the money format. */
    fun requireNonNegativeAmount(field: String, value: BigDecimal): FieldValidator =
        requireAmount(field, value, value.signum() >= 0, NEGATIVE_AMOUNT)

    /** The amount must be greater than zero and fit the money format. */
    fun requirePositiveAmount(field: String, value: BigDecimal): FieldValidator =
        requireAmount(field, value, value.signum() > 0, NOT_POSITIVE_AMOUNT)

    private fun requireAmount(field: String, value: BigDecimal, inRange: Boolean, rangeMessage: String) = apply {
        when {
            !inRange -> violations += FieldViolation(field, rangeMessage)
            !Money.fits(value) -> violations += FieldViolation(field, AMOUNT_FORMAT)
        }
    }

    /** Throws when any rule was broken. */
    fun validate() {
        if (violations.isNotEmpty()) throw ValidationException(violations.toList())
    }

    private companion object {
        const val NEGATIVE_AMOUNT = "must be greater than or equal to 0"
        const val NOT_POSITIVE_AMOUNT = "must be greater than 0"
        const val AMOUNT_FORMAT =
            "must have at most ${Money.MAX_INTEGER_DIGITS} integer digits and ${Money.SCALE} decimal places"
    }
}
