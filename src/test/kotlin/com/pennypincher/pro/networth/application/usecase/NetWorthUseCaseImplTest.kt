package com.pennypincher.pro.networth.application.usecase

import com.pennypincher.pro.networth.domain.model.NetWorthSnapshot
import com.pennypincher.pro.networth.domain.model.NetWorthSummary
import com.pennypincher.pro.networth.domain.model.SnapshotPeriod
import com.pennypincher.pro.networth.domain.model.SnapshotSaveResult
import com.pennypincher.pro.networth.domain.port.outbound.AssetRepository
import com.pennypincher.pro.networth.domain.port.outbound.LiabilityRepository
import com.pennypincher.pro.networth.domain.port.outbound.NetWorthSnapshotRepository
import com.pennypincher.pro.shared.domain.exception.ValidationException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NetWorthUseCaseImplTest {
    private val snapshots = mockk<NetWorthSnapshotRepository>()
    private val assets = mockk<AssetRepository>()
    private val liabilities = mockk<LiabilityRepository>()
    private val useCase = NetWorthUseCaseImpl(snapshots, assets, liabilities, Clock.fixed(NOW, ZoneOffset.UTC))
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun stubTotals() {
        every { assets.sumActive(userId) } returns BigDecimal("411000000.25")
        every { liabilities.sumActive(userId) } returns BigDecimal("42500000")
    }

    @Test
    fun should_calculate_live_net_worth_with_two_decimals() {
        val current = useCase.getCurrentNetWorth(userId)

        assertEquals("411000000.25", current.totalAssets.toPlainString())
        assertEquals("42500000.00", current.totalLiabilities.toPlainString())
        assertEquals("368500000.25", current.netWorth.toPlainString())
    }

    @Test
    fun should_allow_negative_net_worth_when_debts_exceed_assets() {
        every { assets.sumActive(userId) } returns BigDecimal.ZERO

        assertEquals("-42500000.00", useCase.getCurrentNetWorth(userId).netWorth.toPlainString())
    }

    @Test
    fun should_upsert_snapshot_for_first_day_of_month_with_live_totals() {
        val saved = slot<NetWorthSnapshot>()
        every { snapshots.upsert(capture(saved)) } answers { SnapshotSaveResult(firstArg(), created = true) }

        val result = useCase.createOrUpdateSnapshot(userId, LocalDate.parse("2026-10-21"))

        assertTrue(result.created)
        assertEquals(saved.captured, result.snapshot)
        with(saved.captured) {
            assertEquals(userId, this.userId)
            assertEquals(OCTOBER, snapshotDate)
            assertEquals("411000000.25", totalAssets.toPlainString())
            assertEquals("42500000.00", totalLiabilities.toPlainString())
            assertEquals("368500000.25", netWorth.toPlainString())
            assertEquals(NOW, createdAt)
            assertEquals(NOW, updatedAt)
        }
    }

    @Test
    fun should_return_repository_outcome_when_month_already_had_a_snapshot() {
        val existing = NetWorthSnapshot.capture(userId, OCTOBER, NetWorthSummary(BigDecimal.ONE, BigDecimal.ONE), NOW)
        every { snapshots.upsert(any()) } returns SnapshotSaveResult(existing, created = false)

        val result = useCase.createOrUpdateSnapshot(userId, LocalDate.parse("2026-10-31"))

        assertFalse(result.created)
        assertEquals(existing, result.snapshot)
    }

    @Test
    fun should_accept_past_months_and_reject_months_after_the_current_one() {
        every { snapshots.upsert(any()) } answers { SnapshotSaveResult(firstArg(), created = true) }
        assertEquals(
            LocalDate.parse("2021-10-01"),
            useCase.createOrUpdateSnapshot(userId, LocalDate.parse("2021-10-15")).snapshot.snapshotDate,
        )

        val exception = assertThrows<ValidationException> {
            useCase.createOrUpdateSnapshot(userId, LocalDate.parse("2026-11-01"))
        }

        assertEquals(listOf("snapshotDate"), exception.violations.map { it.field })
        verify(exactly = 1) { snapshots.upsert(any()) }
    }

    @Test
    fun should_reject_snapshot_when_a_total_does_not_fit_the_money_format() {
        every { assets.sumActive(userId) } returns BigDecimal("19999999999999999.98")
        every { liabilities.sumActive(userId) } returns BigDecimal("10000000000000000.00")

        val exception = assertThrows<ValidationException> { useCase.createOrUpdateSnapshot(userId, OCTOBER) }

        assertEquals(listOf("totalAssets", "totalLiabilities"), exception.violations.map { it.field })
        verify(exactly = 0) { snapshots.upsert(any()) }
    }

    @Test
    fun should_return_snapshots_oldest_first_within_period() {
        val period = SnapshotPeriod.of(LocalDate.parse("2024-01-15"), LocalDate.parse("2026-12-31"))
        val unordered = listOf("2026-10-01", "2024-01-01", "2025-06-01").map(::snapshotOn)
        every { snapshots.findAll(userId, LocalDate.parse("2024-01-01"), LocalDate.parse("2026-12-31")) } returns
            unordered

        val dates = useCase.listSnapshots(userId, period).map { it.snapshotDate.toString() }

        assertEquals(listOf("2024-01-01", "2025-06-01", "2026-10-01"), dates)
    }

    private fun snapshotOn(date: String) =
        NetWorthSnapshot.capture(userId, LocalDate.parse(date), NetWorthSummary(BigDecimal.ONE, BigDecimal.ONE), NOW)

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-21T14:30:00Z")
        val OCTOBER: LocalDate = LocalDate.parse("2026-10-01")
    }
}
