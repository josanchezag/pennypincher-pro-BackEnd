package com.pennypincher.pro.networth.domain.model

import com.pennypincher.pro.shared.domain.exception.ValidationException
import java.time.LocalDate

/**
 * Inclusive range of snapshot months; a null bound is open. [from] is moved to the first day of its
 * month so that any day of a month includes that month's snapshot.
 */
class SnapshotPeriod private constructor(val from: LocalDate?, val to: LocalDate?) {
    companion object {
        private const val FROM_FIELD = "from"

        fun of(from: LocalDate?, to: LocalDate?): SnapshotPeriod {
            if (from != null && to != null && from.isAfter(to)) {
                throw ValidationException(FROM_FIELD, "must not be after to")
            }
            return SnapshotPeriod(from?.let(NetWorthSnapshot::monthOf), to)
        }
    }
}
