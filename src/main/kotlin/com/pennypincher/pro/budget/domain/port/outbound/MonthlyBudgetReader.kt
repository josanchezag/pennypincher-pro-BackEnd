package com.pennypincher.pro.budget.domain.port.outbound

import com.pennypincher.pro.budget.domain.model.BudgetPeriod
import com.pennypincher.pro.budget.domain.model.MonthlyBudget
import java.util.UUID

/** Read side of the monthly budget persistence; every method is scoped by user. */
interface MonthlyBudgetReader {
    /** Budgets of the user, only those of [year] when given; the order is not defined. */
    fun findAll(userId: UUID, year: Int?): List<MonthlyBudget>

    fun findByPeriod(userId: UUID, period: BudgetPeriod): MonthlyBudget?

    fun findById(userId: UUID, id: UUID): MonthlyBudget?
}
