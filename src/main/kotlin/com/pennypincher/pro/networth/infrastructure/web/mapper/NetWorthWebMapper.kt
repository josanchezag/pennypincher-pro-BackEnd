package com.pennypincher.pro.networth.infrastructure.web.mapper

import com.pennypincher.pro.networth.domain.model.NetWorthSnapshot
import com.pennypincher.pro.networth.domain.model.NetWorthSummary
import com.pennypincher.pro.networth.infrastructure.web.dto.CurrentNetWorthResponse
import com.pennypincher.pro.networth.infrastructure.web.dto.SnapshotResponse

/** Builds the net worth HTTP DTOs from the domain. */
object NetWorthWebMapper {
    fun toResponse(snapshot: NetWorthSnapshot): SnapshotResponse = SnapshotResponse(
        id = snapshot.id,
        snapshotDate = snapshot.snapshotDate,
        totalAssets = snapshot.totalAssets,
        totalLiabilities = snapshot.totalLiabilities,
        netWorth = snapshot.netWorth,
        createdAt = snapshot.createdAt,
        updatedAt = snapshot.updatedAt,
    )

    fun toResponse(summary: NetWorthSummary): CurrentNetWorthResponse = CurrentNetWorthResponse(
        totalAssets = summary.totalAssets,
        totalLiabilities = summary.totalLiabilities,
        netWorth = summary.netWorth,
    )
}
