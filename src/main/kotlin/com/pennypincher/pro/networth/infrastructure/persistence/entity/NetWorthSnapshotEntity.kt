package com.pennypincher.pro.networth.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** JPA mapping of the `net_worth_snapshots` table (one row per user and month). */
@Entity
@Table(name = "net_worth_snapshots")
class NetWorthSnapshotEntity(
    @Id
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "snapshot_date", nullable = false, updatable = false)
    val snapshotDate: LocalDate,
    @Column(name = "total_assets", nullable = false)
    var totalAssets: BigDecimal,
    @Column(name = "total_liabilities", nullable = false)
    var totalLiabilities: BigDecimal,
    @Column(name = "net_worth", nullable = false)
    var netWorth: BigDecimal,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
