package com.pennypincher.pro.expenses.domain.port.outbound

import com.pennypincher.pro.expenses.domain.model.CategoryTotal
import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.shared.domain.port.outbound.UserScopedCrudRepository
import java.util.UUID

/** Persistence port for the expenses of one kind, whose categories are [C]. */
interface ExpenseRepository<C : Enum<C>> : UserScopedCrudRepository<Expense<C>, C> {
    /** Total monthly amount and count of the user's active expenses per category; empty categories are left out. */
    fun totalsByCategory(userId: UUID): List<CategoryTotal<C>>
}
