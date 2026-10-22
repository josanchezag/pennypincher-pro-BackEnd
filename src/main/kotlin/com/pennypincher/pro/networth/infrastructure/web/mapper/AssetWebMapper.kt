package com.pennypincher.pro.networth.infrastructure.web.mapper

import com.pennypincher.pro.networth.domain.model.Asset
import com.pennypincher.pro.networth.domain.model.AssetCommand
import com.pennypincher.pro.networth.infrastructure.web.dto.AssetRequest
import com.pennypincher.pro.networth.infrastructure.web.dto.AssetResponse

/** Converts between the asset HTTP DTOs and the domain. */
object AssetWebMapper {
    /** Expects a request that already passed bean validation, so no field is null. */
    fun toCommand(request: AssetRequest): AssetCommand = AssetCommand(
        name = checkNotNull(request.name),
        value = checkNotNull(request.value),
        category = checkNotNull(request.category),
    )

    fun toResponse(asset: Asset): AssetResponse = AssetResponse(
        id = asset.id,
        name = asset.name,
        value = asset.value,
        category = asset.category,
        createdAt = asset.createdAt,
        updatedAt = asset.updatedAt,
    )
}
