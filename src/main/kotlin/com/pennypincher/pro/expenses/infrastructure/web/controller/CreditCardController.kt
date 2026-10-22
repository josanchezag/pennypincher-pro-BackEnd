package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.domain.model.CreditCard
import com.pennypincher.pro.expenses.domain.model.CreditCardCommand
import com.pennypincher.pro.expenses.domain.port.inbound.CreditCardUseCase
import com.pennypincher.pro.expenses.infrastructure.web.dto.CreditCardRequest
import com.pennypincher.pro.expenses.infrastructure.web.dto.CreditCardResponse
import com.pennypincher.pro.expenses.infrastructure.web.dto.DebtSummaryResponse
import com.pennypincher.pro.expenses.infrastructure.web.mapper.CreditCardWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.AggregateEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.SummaryEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.UncategorizedCrudController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/credit-cards`: the caller's credit cards, plus `/summary`. The endpoints come from the shared bases. */
@RestController
@RequestMapping(ApiPaths.CREDIT_CARDS)
class CreditCardController(useCase: CreditCardUseCase, envelopes: ResponseEnvelopeFactory) :
    UncategorizedCrudController<CreditCard, CreditCardCommand, CreditCardRequest, CreditCardResponse>(
        useCase,
        envelopes,
    ),
    SummaryEndpoint<DebtSummaryResponse> by AggregateEndpoint(envelopes, useCase::summary, DebtSummaryResponse::of) {
    override fun toCommand(request: CreditCardRequest) = CreditCardWebMapper.toCommand(request)

    override fun toResponse(item: CreditCard) = CreditCardWebMapper.toResponse(item)
}
