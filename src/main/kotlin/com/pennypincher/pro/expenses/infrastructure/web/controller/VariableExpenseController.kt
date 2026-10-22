package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.domain.model.enums.VariableExpenseCategory
import com.pennypincher.pro.expenses.domain.port.inbound.ExpenseUseCase
import com.pennypincher.pro.expenses.infrastructure.web.dto.VariableExpenseRequest
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/variable-expenses`: the caller's discretionary monthly budget lines. The endpoints come from [ExpenseController]. */
@RestController
@RequestMapping(ApiPaths.VARIABLE_EXPENSES)
class VariableExpenseController(useCase: ExpenseUseCase<VariableExpenseCategory>, envelopes: ResponseEnvelopeFactory) :
    ExpenseController<VariableExpenseCategory, VariableExpenseRequest>(useCase, envelopes)
