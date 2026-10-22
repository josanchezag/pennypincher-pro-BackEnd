package com.pennypincher.pro.budget.domain.port.inbound

import com.pennypincher.pro.budget.domain.model.IncomeSource
import com.pennypincher.pro.budget.domain.model.IncomeSourceCommand
import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase

/** Managing the caller's income sources; [total] is the total monthly income of the active sources. */
interface IncomeSourceUseCase : CrudUseCase<IncomeSource, IncomeSourceCommand, IncomeCategory>
