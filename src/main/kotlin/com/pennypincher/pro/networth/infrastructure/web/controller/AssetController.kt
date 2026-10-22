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
import com.pennypincher.pro.shared.infrastructure.web.controller.AggregateEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.CategorizedCrudController
import com.pennypincher.pro.shared.infrastructure.web.controller.TotalEndpoint
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/assets`: the caller's assets, plus `/total`. The endpoints themselves come from the shared bases. */
@RestController
@RequestMapping(ApiPaths.ASSETS)
class AssetController(useCase: AssetUseCase, envelopes: ResponseEnvelopeFactory) :
    CategorizedCrudController<Asset, AssetCommand, AssetCategory, AssetRequest, AssetResponse>(useCase, envelopes),
    TotalEndpoint<AssetTotalResponse> by AggregateEndpoint(envelopes, useCase::total, ::AssetTotalResponse) {
    override fun toCommand(request: AssetRequest) = AssetWebMapper.toCommand(request)

    override fun toResponse(item: Asset) = AssetWebMapper.toResponse(item)
}
