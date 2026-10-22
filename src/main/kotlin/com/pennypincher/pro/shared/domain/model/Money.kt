package com.pennypincher.pro.shared.domain.model

import java.math.BigDecimal

/** Rules for COP amounts, stored as `NUMERIC(18,2)`: at most 16 integer digits and 2 decimal places. */
object Money {
    const val SCALE = 2
    const val MAX_INTEGER_DIGITS = 16

    /** Zero with the standard money scale. */
    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(SCALE)

    /** True when [amount] can be stored without rounding or overflow. */
    fun fits(amount: BigDecimal): Boolean {
        val stripped = amount.stripTrailingZeros()
        return stripped.scale() <= SCALE && stripped.precision() - stripped.scale() <= MAX_INTEGER_DIGITS
    }

    /** Returns [amount] with exactly [SCALE] decimal places; it must already [fit][fits]. */
    fun of(amount: BigDecimal): BigDecimal = amount.setScale(SCALE)
}
