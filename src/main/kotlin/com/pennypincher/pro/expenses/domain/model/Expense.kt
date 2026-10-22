package com.pennypincher.pro.expenses.domain.model

import com.pennypincher.pro.shared.domain.model.OwnedResource
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * A recurring monthly expense in COP. Fixed and variable expenses have the same shape and differ only in
 * their category enum [C].
 */
data class Expense<C : Enum<C>>(
    override val id: UUID,
    override val userId: UUID,
    val name: String,
    val monthlyAmount: BigDecimal,
    val category: C,
    val createdAt: Instant,
    val updatedAt: Instant,
) : OwnedResource

/** Data a user supplies to create or update an expense. */
data class ExpenseCommand<C : Enum<C>>(val name: String, val monthlyAmount: BigDecimal, val category: C)
