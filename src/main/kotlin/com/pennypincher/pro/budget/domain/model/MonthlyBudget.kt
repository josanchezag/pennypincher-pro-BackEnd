package com.pennypincher.pro.budget.domain.model

import com.pennypincher.pro.shared.domain.model.OwnedResource
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Values the user enters by hand: the TRM (COP per USD) and the work days of the month; both optional. */
data class BudgetSettings(val exchangeRate: BigDecimal? = null, val workDays: Int? = null) {
    companion object {
        const val MIN_WORK_DAYS = 0
        const val MAX_WORK_DAYS = 31
    }
}

/** The budget of one user for one month, with the figures of its last calculation. */
data class MonthlyBudget(
    override val id: UUID,
    override val userId: UUID,
    val period: BudgetPeriod,
    val settings: BudgetSettings,
    val figures: BudgetFigures,
    val createdAt: Instant,
    val updatedAt: Instant,
) : OwnedResource {
    companion object {
        /** Newest month first. */
        val DISPLAY_ORDER: Comparator<MonthlyBudget> =
            compareByDescending<MonthlyBudget> { it.period.year }.thenByDescending { it.period.month }
    }
}

/** Data a user supplies to create the budget of a month. */
data class CreateBudgetCommand(val year: Int, val month: Int, val settings: BudgetSettings)
