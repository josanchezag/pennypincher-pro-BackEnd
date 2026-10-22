package com.pennypincher.pro.budget.domain.model

import com.pennypincher.pro.shared.domain.validation.FieldValidator

/** The calendar month a budget belongs to. Build it with [of] so that the range rules are checked. */
data class BudgetPeriod(val year: Int, val month: Int) {
    override fun toString(): String = "$year-${month.toString().padStart(MONTH_DIGITS, ZERO_PAD)}"

    companion object {
        const val MIN_YEAR = 2000
        const val MAX_YEAR = 2100
        const val MIN_MONTH = 1
        const val MAX_MONTH = 12
        private const val YEAR_FIELD = "year"
        private const val MONTH_FIELD = "month"
        private const val MONTH_DIGITS = 2
        private const val ZERO_PAD = '0'

        /** A valid period; a year outside [MIN_YEAR]..[MAX_YEAR] or a month outside 1..12 is rejected. */
        fun of(year: Int, month: Int): BudgetPeriod {
            FieldValidator()
                .requireYear(year)
                .require(month in MIN_MONTH..MAX_MONTH, MONTH_FIELD, MONTH_RANGE)
                .validate()
            return BudgetPeriod(year, month)
        }

        /** Rejects a year filter outside [MIN_YEAR]..[MAX_YEAR]. */
        fun requireValidYear(year: Int) = FieldValidator().requireYear(year).validate()

        private fun FieldValidator.requireYear(year: Int) = require(year in MIN_YEAR..MAX_YEAR, YEAR_FIELD, YEAR_RANGE)

        private const val YEAR_RANGE = "must be between $MIN_YEAR and $MAX_YEAR"
        private const val MONTH_RANGE = "must be between $MIN_MONTH and $MAX_MONTH"
    }
}
