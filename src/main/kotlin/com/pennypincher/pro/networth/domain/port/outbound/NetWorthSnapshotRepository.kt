package com.pennypincher.pro.networth.domain.port.outbound

import com.pennypincher.pro.networth.domain.model.NetWorthSnapshot
import com.pennypincher.pro.networth.domain.model.SnapshotSaveResult
import java.time.LocalDate
import java.util.UUID

/** Persistence port for monthly net worth snapshots; every method is scoped by user. */
interface NetWorthSnapshotRepository {
    /** Snapshots whose date is within the inclusive bounds (a null bound is open); the order is not defined. */
    fun findAll(userId: UUID, from: LocalDate?, to: LocalDate?): List<NetWorthSnapshot>

    /**
     * Atomically inserts [snapshot], or, when its user already has a snapshot for that date, replaces that
     * row's totals (keeping its id and creation time). Safe under concurrent calls for the same month.
     */
    fun upsert(snapshot: NetWorthSnapshot): SnapshotSaveResult
}
