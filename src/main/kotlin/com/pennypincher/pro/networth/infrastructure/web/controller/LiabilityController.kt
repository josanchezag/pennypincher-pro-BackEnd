package com.pennypincher.pro.networth.infrastructure.web.controller

import com.pennypincher.pro.networth.domain.model.Liability
import com.pennypincher.pro.networth.domain.model.LiabilityCommand
import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import com.pennypincher.pro.networth.domain.port.inbound.LiabilityUseCase
import com.pennypincher.pro.networth.infrastructure.web.dto.LiabilityRequest
import com.pennypincher.pro.networth.infrastructure.web.dto.LiabilityResponse
import com.pennypincher.pro.networth.infrastructure.web.dto.LiabilityTotalResponse
import com.pennypincher.pro.networth.infrastructure.web.mapper.LiabilityWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.AggregateEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.CategorizedCrudController
import com.pennypincher.pro.shared.infrastructure.web.controller.TotalEndpoint
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/liabilities`: the caller's debts, plus `/total`. The endpoints themselves come from the shared bases. */
@RestController
@RequestMapping(ApiPaths.LIABILITIES)
class LiabilityController(useCase: LiabilityUseCase, envelopes: ResponseEnvelopeFactory) :
    CategorizedCrudController<Liability, LiabilityCommand, LiabilityCategory, LiabilityRequest, LiabilityResponse>(
        useCase,
        envelopes,
    ),
    TotalEndpoint<LiabilityTotalResponse> by AggregateEndpoint(envelopes, useCase::total, ::LiabilityTotalResponse) {
    override fun toCommand(request: LiabilityRequest) = LiabilityWebMapper.toCommand(request)

    override fun toResponse(item: Liability) = LiabilityWebMapper.toResponse(item)
}
