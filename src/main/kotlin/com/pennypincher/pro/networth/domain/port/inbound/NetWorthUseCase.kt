package com.pennypincher.pro.networth.domain.port.inbound

import com.pennypincher.pro.networth.domain.model.NetWorthSnapshot
import com.pennypincher.pro.networth.domain.model.NetWorthSummary
import com.pennypincher.pro.networth.domain.model.SnapshotPeriod
import com.pennypincher.pro.networth.domain.model.SnapshotSaveResult
import java.time.LocalDate
import java.util.UUID

/** Net worth of the caller: live totals and monthly snapshots. `userId` always comes from the access token. */
interface NetWorthUseCase {
    /** Totals of the active assets and liabilities right now (nothing is stored). */
    fun getCurrentNetWorth(userId: UUID): NetWorthSummary

    /** Stores the current totals for the month of [snapshotDate], replacing that month's snapshot if any. */
    fun createOrUpdateSnapshot(userId: UUID, snapshotDate: LocalDate): SnapshotSaveResult

    /** Snapshots within [period], oldest first. */
    fun listSnapshots(userId: UUID, period: SnapshotPeriod): List<NetWorthSnapshot>
}
