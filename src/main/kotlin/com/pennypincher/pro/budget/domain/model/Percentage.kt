package com.pennypincher.pro.budget.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Share of a whole in percent units (`25.00` means a quarter), rounded half up. */
object Percentage {
    /** Decimal places kept in storage. */
    const val STORED_SCALE = 4

    /** Decimal places shown to the user. */
    const val DISPLAY_SCALE = 2

    /** Largest share kept; a part far bigger than its whole is capped here so it still fits the storage. */
    val MAX: BigDecimal = BigDecimal("99999.99")

    private val HUNDRED = BigDecimal("100")

    /** [part] / [whole] × 100 with [scale] decimals; zero when [whole] is zero. */
    fun of(part: BigDecimal, whole: BigDecimal, scale: Int): BigDecimal {
        if (whole.signum() == 0) return BigDecimal.ZERO.setScale(scale)
        return part.multiply(HUNDRED).divide(whole, scale, RoundingMode.HALF_UP).min(MAX.setScale(scale))
    }
}
