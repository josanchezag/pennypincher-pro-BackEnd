package com.pennypincher.pro.networth.infrastructure.web.dto

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Public representation of a monthly snapshot; `snapshotDate` is the first day of the month. */
data class SnapshotResponse(
    val id: UUID,
    val snapshotDate: LocalDate,
    val totalAssets: BigDecimal,
    val totalLiabilities: BigDecimal,
    val netWorth: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
)
