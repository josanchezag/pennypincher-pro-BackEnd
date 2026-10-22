package com.pennypincher.pro.expenses.infrastructure.persistence.adapter

import com.pennypincher.pro.expenses.domain.model.CategoryTotal
import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.expenses.domain.port.outbound.ExpenseRepository
import com.pennypincher.pro.shared.infrastructure.persistence.adapter.CategorizedAmountRepositoryAdapter
import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import com.pennypincher.pro.shared.infrastructure.persistence.repository.CategorizedAmountJpaRepository
import java.util.UUID

/**
 * Adapts the table of one kind of expense to the domain [ExpenseRepository] port. Reading is the same for
 * every kind; subclasses supply the JPA repository and the category enum, and build their own entity in
 * `toEntity` with named arguments.
 */
abstract class ExpenseRepositoryAdapter<E : CategorizedAmountEntity, C : Enum<C>>(
    private val jpaRepository: CategorizedAmountJpaRepository<E>,
    private val categoryType: Class<C>,
) : CategorizedAmountRepositoryAdapter<Expense<C>, E, C>(jpaRepository),
    ExpenseRepository<C> {
    override fun toDomain(entity: E) = Expense(
        id = entity.id,
        userId = entity.userId,
        name = entity.name,
        monthlyAmount = entity.amount,
        category = categoryOf(entity.category),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun totalsByCategory(userId: UUID): List<CategoryTotal<C>> =
        jpaRepository.sumActiveAmountByCategory(userId).map {
            CategoryTotal(categoryOf(it.category), it.totalAmount, it.count)
        }

    private fun categoryOf(name: String): C = java.lang.Enum.valueOf(categoryType, name)
}
