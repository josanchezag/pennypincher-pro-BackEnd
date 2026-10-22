package com.pennypincher.pro.networth.infrastructure.web.dto

import java.math.BigDecimal

/** Body of `GET /net-worth/current`: live totals, not a stored snapshot. */
data class CurrentNetWorthResponse(
    val totalAssets: BigDecimal,
    val totalLiabilities: BigDecimal,
    val netWorth: BigDecimal,
)
