package com.pennypincher.pro.budget.domain.exception

import com.pennypincher.pro.budget.domain.model.BudgetPeriod
import com.pennypincher.pro.shared.domain.exception.BusinessException
import com.pennypincher.pro.shared.domain.exception.ErrorCode

/** The user already has a budget for that month; there is at most one per month. */
class BudgetAlreadyExistsException(period: BudgetPeriod) :
    BusinessException(ErrorCode.CONFLICT, "A budget for $period already exists")
