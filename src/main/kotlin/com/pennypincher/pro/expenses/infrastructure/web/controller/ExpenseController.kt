package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.expenses.domain.model.ExpenseCommand
import com.pennypincher.pro.expenses.domain.port.inbound.ExpenseUseCase
import com.pennypincher.pro.expenses.infrastructure.web.dto.ExpenseRequest
import com.pennypincher.pro.expenses.infrastructure.web.dto.ExpenseResponse
import com.pennypincher.pro.expenses.infrastructure.web.dto.ExpenseSummaryResponse
import com.pennypincher.pro.expenses.infrastructure.web.mapper.ExpenseWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.AggregateEndpoint
import com.pennypincher.pro.shared.infrastructure.web.controller.CategorizedCrudController
import com.pennypincher.pro.shared.infrastructure.web.controller.SummaryEndpoint

/**
 * Endpoints of one kind of expense with categories [C] and request body [Req]: CRUD with the `?category=`
 * filter, plus `/summary`. Concrete controllers only add `@RestController` and `@RequestMapping`.
 */
abstract class ExpenseController<C : Enum<C>, Req : ExpenseRequest<C>>(
    useCase: ExpenseUseCase<C>,
    envelopes: ResponseEnvelopeFactory,
) : CategorizedCrudController<Expense<C>, ExpenseCommand<C>, C, Req, ExpenseResponse<C>>(useCase, envelopes),
    SummaryEndpoint<ExpenseSummaryResponse<C>> by AggregateEndpoint(
        envelopes,
        useCase::summary,
        ExpenseWebMapper::toSummaryResponse,
    ) {
    override fun toCommand(request: Req) = ExpenseWebMapper.toCommand(request)

    override fun toResponse(item: Expense<C>) = ExpenseWebMapper.toResponse(item)
}
