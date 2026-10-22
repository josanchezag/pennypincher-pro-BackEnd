package com.pennypincher.pro.networth.infrastructure.web.dto

import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/** Body of `POST /net-worth/snapshots`; any day of the month selects that month. */
data class SnapshotRequest(
    @field:NotNull(message = RequestMessages.REQUIRED)
    val snapshotDate: LocalDate? = null,
)
