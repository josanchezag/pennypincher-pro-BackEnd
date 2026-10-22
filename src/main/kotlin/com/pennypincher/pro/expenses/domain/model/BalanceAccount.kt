package com.pennypincher.pro.expenses.domain.model

import com.pennypincher.pro.shared.domain.model.OwnedResource
import java.math.BigDecimal

/**
 * Shape shared by credit cards, loans and savings accounts: a named record of the user with a monthly
 * amount (a payment or a contribution) and a balance, in COP, without category.
 */
interface BalanceAccount : OwnedResource {
    val name: String
}

/** Data a user supplies for a [BalanceAccount]; the amounts and their field names differ per kind. */
interface BalanceAccountCommand {
    val name: String
}

/** Sums over the active [BalanceAccount]s of one kind of a user, and how many there are. */
data class BalanceTotals(val totalMonthlyAmount: BigDecimal, val totalBalance: BigDecimal, val count: Long)
