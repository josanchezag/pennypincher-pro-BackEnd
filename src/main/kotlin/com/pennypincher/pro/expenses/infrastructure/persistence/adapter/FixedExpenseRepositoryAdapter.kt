package com.pennypincher.pro.expenses.infrastructure.persistence.adapter

import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.expenses.domain.model.enums.FixedExpenseCategory
import com.pennypincher.pro.expenses.infrastructure.persistence.entity.FixedExpenseEntity
import com.pennypincher.pro.expenses.infrastructure.persistence.repository.FixedExpenseJpaRepository
import org.springframework.stereotype.Component

/** Fixed expenses for the domain `ExpenseRepository<FixedExpenseCategory>` port. Only active rows are read. */
@Component
class FixedExpenseRepositoryAdapter(jpaRepository: FixedExpenseJpaRepository) :
    ExpenseRepositoryAdapter<FixedExpenseEntity, FixedExpenseCategory>(
        jpaRepository,
        FixedExpenseCategory::class.java,
    ) {
    override fun toEntity(item: Expense<FixedExpenseCategory>) = FixedExpenseEntity(
        id = item.id,
        userId = item.userId,
        name = item.name,
        monthlyAmount = item.monthlyAmount,
        category = item.category.name,
        active = true,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
