package com.pennypincher.pro.expenses.infrastructure.web.dto

import com.pennypincher.pro.expenses.domain.model.BalanceTotals
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Public representation of a savings account. */
data class SavingsAccountResponse(
    val id: UUID,
    val name: String,
    val monthlyContribution: BigDecimal,
    val currentBalance: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Body of `GET /savings-accounts/summary`. */
data class SavingsSummaryResponse(
    val totalMonthlyContributions: BigDecimal,
    val totalCurrentBalance: BigDecimal,
    val count: Long,
) {
    companion object {
        fun of(totals: BalanceTotals) =
            SavingsSummaryResponse(totals.totalMonthlyAmount, totals.totalBalance, totals.count)
    }
}
