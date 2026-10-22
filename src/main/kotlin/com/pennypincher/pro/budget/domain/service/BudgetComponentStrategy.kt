package com.pennypincher.pro.budget.domain.service

import com.pennypincher.pro.budget.domain.model.BudgetSourceAmounts
import com.pennypincher.pro.budget.domain.model.enums.BudgetComponent
import java.math.BigDecimal

/** Strategy that knows which sources make up the monthly amount of one [component]. */
interface BudgetComponentStrategy {
    val component: BudgetComponent

    /** Monthly amount of [component] taken from the user's current [sources]. */
    fun monthlyAmount(sources: BudgetSourceAmounts): BigDecimal
}
