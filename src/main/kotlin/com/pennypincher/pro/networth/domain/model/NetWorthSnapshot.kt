package com.pennypincher.pro.networth.domain.model

import com.pennypincher.pro.shared.domain.model.OwnedResource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Net worth of one user for one month. [snapshotDate] is always the first day of that month. */
data class NetWorthSnapshot(
    override val id: UUID,
    override val userId: UUID,
    val snapshotDate: LocalDate,
    val summary: NetWorthSummary,
    val createdAt: Instant,
    val updatedAt: Instant,
) : OwnedResource {
    val totalAssets: BigDecimal get() = summary.totalAssets
    val totalLiabilities: BigDecimal get() = summary.totalLiabilities
    val netWorth: BigDecimal get() = summary.netWorth

    companion object {
        /** The month a date belongs to, as the first day of that month. */
        fun monthOf(date: LocalDate): LocalDate = date.withDayOfMonth(1)

        /** A new snapshot for the month that contains [date]. */
        fun capture(userId: UUID, date: LocalDate, summary: NetWorthSummary, now: Instant): NetWorthSnapshot =
            NetWorthSnapshot(UUID.randomUUID(), userId, monthOf(date), summary, now, now)
    }
}

/**
 * Outcome of saving a monthly snapshot: [created] is false when an existing month was recalculated (its
 * id and creation time are kept).
 */
data class SnapshotSaveResult(val snapshot: NetWorthSnapshot, val created: Boolean)
