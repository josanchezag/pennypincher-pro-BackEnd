package com.pennypincher.pro.expenses.infrastructure.web.dto

import com.pennypincher.pro.expenses.domain.model.BalanceTotals
import java.math.BigDecimal

/** Body of `GET /credit-cards/summary` and `GET /loans/summary`. */
data class DebtSummaryResponse(
    val totalMonthlyPayments: BigDecimal,
    val totalOutstandingBalance: BigDecimal,
    val count: Long,
) {
    companion object {
        fun of(totals: BalanceTotals) =
            DebtSummaryResponse(totals.totalMonthlyAmount, totals.totalBalance, totals.count)
    }
}
