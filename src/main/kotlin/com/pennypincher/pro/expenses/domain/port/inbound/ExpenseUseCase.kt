package com.pennypincher.pro.expenses.domain.port.inbound

import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.expenses.domain.model.ExpenseCommand
import com.pennypincher.pro.expenses.domain.model.ExpenseSummary
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase
import com.pennypincher.pro.shared.domain.port.inbound.SummaryUseCase

/** Managing the caller's expenses of one kind (fixed or variable), plus their per-category summary. */
interface ExpenseUseCase<C : Enum<C>> :
    CrudUseCase<Expense<C>, ExpenseCommand<C>, C>,
    SummaryUseCase<ExpenseSummary<C>>
