package com.pennypincher.pro.budget.infrastructure.web.controller

import com.pennypincher.pro.budget.domain.model.IncomeSource
import com.pennypincher.pro.budget.domain.model.IncomeSourceCommand
import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import com.pennypincher.pro.budget.domain.port.inbound.IncomeSourceUseCase
import com.pennypincher.pro.budget.infrastructure.web.dto.IncomeSourceRequest
import com.pennypincher.pro.budget.infrastructure.web.dto.IncomeSourceResponse
import com.pennypincher.pro.budget.infrastructure.web.dto.IncomeSourceTotalResponse
import com.pennypincher.pro.budget.infrastructure.web.mapper.IncomeSourceWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.AggregateEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.CategorizedCrudController
import com.pennypincher.pro.shared.infrastructure.web.controller.TotalEndpoint
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/income-sources`: the caller's income streams, plus `/total`. The endpoints come from the shared bases. */
@RestController
@RequestMapping(ApiPaths.INCOME_SOURCES)
class IncomeSourceController(useCase: IncomeSourceUseCase, envelopes: ResponseEnvelopeFactory) :
    CategorizedCrudController<
        IncomeSource,
        IncomeSourceCommand,
        IncomeCategory,
        IncomeSourceRequest,
        IncomeSourceResponse,
        >(useCase, envelopes),
    TotalEndpoint<IncomeSourceTotalResponse> by AggregateEndpoint(
        envelopes,
        useCase::total,
        ::IncomeSourceTotalResponse,
    ) {
    override fun toCommand(request: IncomeSourceRequest) = IncomeSourceWebMapper.toCommand(request)

    override fun toResponse(item: IncomeSource) = IncomeSourceWebMapper.toResponse(item)
}
