package com.pennypincher.pro.networth.domain.model

import com.pennypincher.pro.shared.domain.exception.ValidationException
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SnapshotPeriodTest {
    @Test
    fun should_move_from_to_first_day_of_month_and_keep_to_when_range_is_valid() {
        val period = SnapshotPeriod.of(LocalDate.parse("2024-01-15"), LocalDate.parse("2026-12-31"))

        assertEquals(LocalDate.parse("2024-01-01"), period.from)
        assertEquals(LocalDate.parse("2026-12-31"), period.to)
    }

    @Test
    fun should_leave_bounds_open_when_they_are_absent() {
        val period = SnapshotPeriod.of(null, null)

        assertNull(period.from)
        assertNull(period.to)
        assertEquals(LocalDate.parse("2026-10-01"), SnapshotPeriod.of(LocalDate.parse("2026-10-09"), null).from)
    }

    @Test
    fun should_accept_same_day_and_reject_from_after_to() {
        val day = LocalDate.parse("2026-10-01")
        assertEquals(day, SnapshotPeriod.of(day, day).to)

        val exception = assertThrows<ValidationException> { SnapshotPeriod.of(day.plusDays(1), day) }

        assertEquals("from", exception.violations.single().field)
    }
}
