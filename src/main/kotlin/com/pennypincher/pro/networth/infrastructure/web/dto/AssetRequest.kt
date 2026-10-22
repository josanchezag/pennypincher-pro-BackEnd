package com.pennypincher.pro.networth.infrastructure.web.dto

import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.shared.infrastructure.web.dto.RequestMessages
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/** Body of `POST /assets` and `PUT /assets/{id}`; the business rules are checked by the use case. */
data class AssetRequest(
    @field:NotNull(message = RequestMessages.REQUIRED)
    val name: String? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val value: BigDecimal? = null,
    @field:NotNull(message = RequestMessages.REQUIRED)
    val category: AssetCategory? = null,
)
