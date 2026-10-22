package com.pennypincher.pro.shared.domain.port.inbound

import java.math.BigDecimal
import java.util.UUID

/** Records with a single amount whose active total the caller can ask for. */
interface TotalUseCase {
    /** Sum of the amounts of the caller's active records, with two decimal places. */
    fun total(userId: UUID): BigDecimal
}
