package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.domain.model.Loan
import com.pennypincher.pro.expenses.domain.model.LoanCommand
import com.pennypincher.pro.expenses.domain.port.inbound.LoanUseCase
import com.pennypincher.pro.expenses.infrastructure.web.dto.DebtSummaryResponse
import com.pennypincher.pro.expenses.infrastructure.web.dto.LoanRequest
import com.pennypincher.pro.expenses.infrastructure.web.dto.LoanResponse
import com.pennypincher.pro.expenses.infrastructure.web.mapper.LoanWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.AggregateEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.SummaryEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.UncategorizedCrudController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/loans`: the caller's loans, plus `/summary`. The endpoints come from the shared bases. */
@RestController
@RequestMapping(ApiPaths.LOANS)
class LoanController(useCase: LoanUseCase, envelopes: ResponseEnvelopeFactory) :
    UncategorizedCrudController<Loan, LoanCommand, LoanRequest, LoanResponse>(useCase, envelopes),
    SummaryEndpoint<DebtSummaryResponse> by AggregateEndpoint(envelopes, useCase::summary, DebtSummaryResponse::of) {
    override fun toCommand(request: LoanRequest) = LoanWebMapper.toCommand(request)

    override fun toResponse(item: Loan) = LoanWebMapper.toResponse(item)
}
