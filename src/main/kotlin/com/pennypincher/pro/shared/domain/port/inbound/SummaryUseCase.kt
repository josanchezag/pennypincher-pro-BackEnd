package com.pennypincher.pro.shared.domain.port.inbound

import java.util.UUID

/** Records whose active rows the caller can see summarized as [S] (totals, counts, breakdowns). */
interface SummaryUseCase<S> {
    /** Summary of the caller's active records, computed by the database. */
    fun summary(userId: UUID): S
}
