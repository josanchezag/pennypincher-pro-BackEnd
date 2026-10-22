package com.pennypincher.pro.networth.domain.model

import java.math.BigDecimal

/** Totals of a balance sheet; the net worth can be negative when debts exceed assets. */
data class NetWorthSummary(val totalAssets: BigDecimal, val totalLiabilities: BigDecimal) {
    val netWorth: BigDecimal get() = totalAssets - totalLiabilities
}
