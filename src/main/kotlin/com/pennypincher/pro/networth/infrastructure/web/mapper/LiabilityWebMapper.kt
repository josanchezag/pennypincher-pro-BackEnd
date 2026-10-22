package com.pennypincher.pro.networth.infrastructure.web.mapper

import com.pennypincher.pro.networth.domain.model.Liability
import com.pennypincher.pro.networth.domain.model.LiabilityCommand
import com.pennypincher.pro.networth.infrastructure.web.dto.LiabilityRequest
import com.pennypincher.pro.networth.infrastructure.web.dto.LiabilityResponse

/** Converts between the liability HTTP DTOs and the domain. */
object LiabilityWebMapper {
    /** Expects a request that already passed bean validation, so no field is null. */
    fun toCommand(request: LiabilityRequest): LiabilityCommand = LiabilityCommand(
        name = checkNotNull(request.name),
        currentBalance = checkNotNull(request.currentBalance),
        category = checkNotNull(request.category),
    )

    fun toResponse(liability: Liability): LiabilityResponse = LiabilityResponse(
        id = liability.id,
        name = liability.name,
        currentBalance = liability.currentBalance,
        category = liability.category,
        createdAt = liability.createdAt,
        updatedAt = liability.updatedAt,
    )
}
