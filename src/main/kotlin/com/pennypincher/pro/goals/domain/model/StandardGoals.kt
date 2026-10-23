package com.pennypincher.pro.goals.domain.model

import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import java.math.BigDecimal

/** The four pre-configured goals of the "Metas financieras" spreadsheet, one per [GoalLevel]. */
object StandardGoals {
    private val ANNUAL_RETURN = BigDecimal("0.11")
    private const val TARGET_YEARS = 16

    val ALL: List<FinancialGoalCommand> = listOf(
        standard("Seguridad Financiera", GoalLevel.FINANCIAL_SECURITY, "605000000", "1470000"),
        standard("Independencia Financiera", GoalLevel.FINANCIAL_INDEPENDENCE, "1016000000", "3040000"),
        standard("Libertad Financiera", GoalLevel.FINANCIAL_FREEDOM, "2066000000", "7000000"),
        standard("Libertad Absoluta", GoalLevel.ABSOLUTE_FREEDOM, "4366000000", "15850000"),
    )

    private fun standard(name: String, level: GoalLevel, target: String, monthlyInvestment: String) =
        FinancialGoalCommand(
            name,
            level,
            BigDecimal(target),
            BigDecimal(monthlyInvestment),
            ANNUAL_RETURN,
            TARGET_YEARS,
        )
}
