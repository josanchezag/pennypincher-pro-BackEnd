package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.domain.model.SavingsAccount
import com.pennypincher.pro.expenses.domain.model.SavingsAccountCommand
import com.pennypincher.pro.expenses.domain.port.inbound.SavingsAccountUseCase
import com.pennypincher.pro.expenses.infrastructure.web.dto.SavingsAccountRequest
import com.pennypincher.pro.expenses.infrastructure.web.dto.SavingsAccountResponse
import com.pennypincher.pro.expenses.infrastructure.web.dto.SavingsSummaryResponse
import com.pennypincher.pro.expenses.infrastructure.web.mapper.SavingsAccountWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.AggregateEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.SummaryEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.UncategorizedCrudController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/savings-accounts`: the caller's savings, plus `/summary`. The endpoints come from the shared bases. */
@RestController
@RequestMapping(ApiPaths.SAVINGS_ACCOUNTS)
class SavingsAccountController(useCase: SavingsAccountUseCase, envelopes: ResponseEnvelopeFactory) :
    UncategorizedCrudController<SavingsAccount, SavingsAccountCommand, SavingsAccountRequest, SavingsAccountResponse>(
        useCase,
        envelopes,
    ),
    SummaryEndpoint<SavingsSummaryResponse> by AggregateEndpoint(
        envelopes,
        useCase::summary,
        SavingsSummaryResponse::of,
    ) {
    override fun toCommand(request: SavingsAccountRequest) = SavingsAccountWebMapper.toCommand(request)

    override fun toResponse(item: SavingsAccount) = SavingsAccountWebMapper.toResponse(item)
}
