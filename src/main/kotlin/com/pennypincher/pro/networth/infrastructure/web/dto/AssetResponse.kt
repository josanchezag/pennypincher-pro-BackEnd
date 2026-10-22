package com.pennypincher.pro.networth.infrastructure.web.dto

import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Public representation of an asset. */
data class AssetResponse(
    val id: UUID,
    val name: String,
    val value: BigDecimal,
    val category: AssetCategory,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Body of `GET /assets/total`. */
data class AssetTotalResponse(val totalAssets: BigDecimal)
