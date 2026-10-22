package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.domain.model.enums.FixedExpenseCategory
import com.pennypincher.pro.expenses.domain.port.inbound.ExpenseUseCase
import com.pennypincher.pro.expenses.infrastructure.web.dto.FixedExpenseRequest
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/fixed-expenses`: the caller's recurring fixed costs. The endpoints come from [ExpenseController]. */
@RestController
@RequestMapping(ApiPaths.FIXED_EXPENSES)
class FixedExpenseController(useCase: ExpenseUseCase<FixedExpenseCategory>, envelopes: ResponseEnvelopeFactory) :
    ExpenseController<FixedExpenseCategory, FixedExpenseRequest>(useCase, envelopes)
