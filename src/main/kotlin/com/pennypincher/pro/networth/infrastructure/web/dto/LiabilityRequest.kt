package com.pennypincher.pro.networth.infrastructure.web.dto

import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/** Body of `POST /liabilities` and `PUT /liabilities/{id}`; the business rules are checked by the use case. */
data class LiabilityRequest(
    @field:NotNull(message = RequestMessages.REQUIRED)
    val name: String? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val currentBalance: BigDecimal? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val category: LiabilityCategory? = null,
)
