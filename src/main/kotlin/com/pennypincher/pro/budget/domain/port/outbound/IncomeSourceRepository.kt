package com.pennypincher.pro.budget.domain.port.outbound

import com.pennypincher.pro.budget.domain.model.IncomeSource
import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import com.pennypincher.pro.shared.domain.port.outbound.SummableCrudRepository

/** Persistence port for income sources; [sumActive] adds up the monthly amounts. */
interface IncomeSourceRepository : SummableCrudRepository<IncomeSource, IncomeCategory>
