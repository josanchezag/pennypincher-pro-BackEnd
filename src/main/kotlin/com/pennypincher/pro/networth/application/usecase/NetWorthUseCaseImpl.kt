package com.pennypincher.pro.networth.application.usecase

import com.pennypincher.pro.networth.domain.model.NetWorthSnapshot
import com.pennypincher.pro.networth.domain.model.NetWorthSummary
import com.pennypincher.pro.networth.domain.model.SnapshotPeriod
import com.pennypincher.pro.networth.domain.model.SnapshotSaveResult
import com.pennypincher.pro.networth.domain.port.inbound.NetWorthUseCase
import com.pennypincher.pro.networth.domain.port.outbound.AssetRepository
import com.pennypincher.pro.networth.domain.port.outbound.LiabilityRepository
import com.pennypincher.pro.networth.domain.port.outbound.NetWorthSnapshotRepository
import com.pennypincher.pro.shared.domain.model.Money
import com.pennypincher.pro.shared.domain.validation.FieldValidator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Net worth use cases. A month has at most one snapshot; saving it again recalculates it (idempotent).
 * Months after the current one (per the clock) and totals that do not fit the money format are rejected.
 */
class NetWorthUseCaseImpl(
    private val snapshotRepository: NetWorthSnapshotRepository,
    private val assetRepository: AssetRepository,
    private val liabilityRepository: LiabilityRepository,
    private val clock: Clock,
) : NetWorthUseCase {
    override fun getCurrentNetWorth(userId: UUID): NetWorthSummary = NetWorthSummary(
        totalAssets = Money.of(assetRepository.sumActive(userId)),
        totalLiabilities = Money.of(liabilityRepository.sumActive(userId)),
    )

    override fun createOrUpdateSnapshot(userId: UUID, snapshotDate: LocalDate): SnapshotSaveResult {
        val summary = getCurrentNetWorth(userId)
        val now = Instant.now(clock)
        val month = NetWorthSnapshot.monthOf(snapshotDate)
        FieldValidator()
            .require(!month.isAfter(currentMonth(now)), SNAPSHOT_DATE_FIELD, FUTURE_MONTH)
            .requireNonNegativeAmount(TOTAL_ASSETS_FIELD, summary.totalAssets)
            .requireNonNegativeAmount(TOTAL_LIABILITIES_FIELD, summary.totalLiabilities)
            .validate()
        return snapshotRepository.upsert(NetWorthSnapshot.capture(userId, month, summary, now))
    }

    override fun listSnapshots(userId: UUID, period: SnapshotPeriod): List<NetWorthSnapshot> =
        snapshotRepository.findAll(userId, period.from, period.to).sortedBy(NetWorthSnapshot::snapshotDate)

    private fun currentMonth(now: Instant): LocalDate = NetWorthSnapshot.monthOf(LocalDate.ofInstant(now, clock.zone))

    private companion object {
        const val SNAPSHOT_DATE_FIELD = "snapshotDate"
        const val TOTAL_ASSETS_FIELD = "totalAssets"
        const val TOTAL_LIABILITIES_FIELD = "totalLiabilities"
        const val FUTURE_MONTH = "must not be after the current month"
    }
}
