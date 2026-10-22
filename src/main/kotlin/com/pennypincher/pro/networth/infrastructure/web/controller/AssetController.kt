package com.pennypincher.pro.networth.infrastructure.web.controller

import com.pennypincher.pro.networth.domain.model.Asset
import com.pennypincher.pro.networth.domain.model.AssetCommand
import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.networth.domain.port.inbound.AssetUseCase
import com.pennypincher.pro.networth.infrastructure.web.dto.AssetRequest
import com.pennypincher.pro.networth.infrastructure.web.dto.AssetResponse
import com.pennypincher.pro.networth.infrastructure.web.dto.AssetTotalResponse
import com.pennypincher.pro.networth.infrastructure.web.mapper.AssetWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.AbstractCrudController
import java.math.BigDecimal
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/assets`: the caller's assets. The endpoints themselves are defined by [AbstractCrudController]. */
@RestController
@RequestMapping(ApiPaths.ASSETS)
class AssetController(useCase: AssetUseCase, envelopes: ResponseEnvelopeFactory) :
    AbstractCrudController<Asset, AssetCommand, AssetCategory, AssetRequest, AssetResponse>(useCase, envelopes) {
    override fun toCommand(request: AssetRequest) = AssetWebMapper.toCommand(request)

    override fun toResponse(item: Asset) = AssetWebMapper.toResponse(item)

    override fun toTotalResponse(total: BigDecimal) = AssetTotalResponse(total)
}
