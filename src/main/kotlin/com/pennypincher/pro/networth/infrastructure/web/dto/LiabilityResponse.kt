package com.pennypincher.pro.networth.infrastructure.web.dto

import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Public representation of a liability. */
data class LiabilityResponse(
    val id: UUID,
    val name: String,
    val currentBalance: BigDecimal,
    val category: LiabilityCategory,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Body of `GET /liabilities/total`. */
data class LiabilityTotalResponse(val totalLiabilities: BigDecimal)
