package com.pennypincher.pro.goals.domain.service

import java.math.BigDecimal
import java.math.MathContext

/**
 * Future value of a balance compounded monthly at the monthly equivalent of a yearly return, with a fixed
 * contribution at the end of each month: `FV(n) = PV·(1+r)^n + PMT·((1+r)^n − 1)/r`. Everything is computed
 * with [MathContext.DECIMAL128]; callers round the results.
 */
internal object CompoundGrowth {
    /** 34 significant digits: 600 months of compounding stay exact to the cent before the final rounding. */
    private val CONTEXT = MathContext.DECIMAL128
    const val MONTHS_PER_YEAR = 12
    private const val NEWTON_STEPS = 2
    private val ROOT_DEGREE = BigDecimal(MONTHS_PER_YEAR)
    private val ROOT_DEGREE_MINUS_ONE = BigDecimal(MONTHS_PER_YEAR - 1)

    /** Monthly rate `r = (1 + annual)^(1/12) − 1`, the effective equivalent of [annualReturn]. */
    fun monthlyRate(annualReturn: BigDecimal): BigDecimal {
        val base = BigDecimal.ONE.add(annualReturn)
        val estimate = BigDecimal(Math.pow(base.toDouble(), 1.0 / MONTHS_PER_YEAR), CONTEXT)
        val root = (1..NEWTON_STEPS).fold(estimate) { approximation, _ -> newtonStep(approximation, base) }
        return root.subtract(BigDecimal.ONE, CONTEXT)
    }

    /** `FV(0)` to `FV(lastMonth)`; `FV(0)` is [presentValue]. [annualReturn] must be positive. */
    fun futureValues(
        presentValue: BigDecimal,
        monthlyPayment: BigDecimal,
        annualReturn: BigDecimal,
        lastMonth: Int,
    ): List<BigDecimal> {
        val rate = monthlyRate(annualReturn)
        val factor = BigDecimal.ONE.add(rate, CONTEXT)
        return generateSequence(BigDecimal.ONE) { it.multiply(factor, CONTEXT) }
            .take(lastMonth + 1)
            .map { growth ->
                val contributions = monthlyPayment.multiply(growth.subtract(BigDecimal.ONE, CONTEXT), CONTEXT)
                presentValue.multiply(growth, CONTEXT).add(contributions.divide(rate, CONTEXT), CONTEXT)
            }
            .toList()
    }

    /** One Newton iteration towards the twelfth root of [base]: `x ← (11·x + base / x^11) / 12`. */
    private fun newtonStep(root: BigDecimal, base: BigDecimal): BigDecimal {
        val quotient = base.divide(root.pow(MONTHS_PER_YEAR - 1, CONTEXT), CONTEXT)
        val weighted = root.multiply(ROOT_DEGREE_MINUS_ONE, CONTEXT).add(quotient, CONTEXT)
        return weighted.divide(ROOT_DEGREE, CONTEXT)
    }
}
