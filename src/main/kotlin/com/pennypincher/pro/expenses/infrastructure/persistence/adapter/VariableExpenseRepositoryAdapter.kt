package com.pennypincher.pro.expenses.infrastructure.persistence.adapter

import com.pennypincher.pro.expenses.domain.model.Expense
import com.pennypincher.pro.expenses.domain.model.enums.VariableExpenseCategory
import com.pennypincher.pro.expenses.infrastructure.persistence.entity.VariableExpenseEntity
import com.pennypincher.pro.expenses.infrastructure.persistence.repository.VariableExpenseJpaRepository
import org.springframework.stereotype.Component

/** Variable expenses for the domain `ExpenseRepository<VariableExpenseCategory>` port. Only active rows are read. */
@Component
class VariableExpenseRepositoryAdapter(jpaRepository: VariableExpenseJpaRepository) :
    ExpenseRepositoryAdapter<VariableExpenseEntity, VariableExpenseCategory>(
        jpaRepository,
        VariableExpenseCategory::class.java,
    ) {
    override fun toEntity(item: Expense<VariableExpenseCategory>) = VariableExpenseEntity(
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
