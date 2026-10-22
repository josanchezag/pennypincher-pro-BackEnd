package com.pennypincher.pro.budget.infrastructure.persistence.repository

import com.pennypincher.pro.budget.infrastructure.persistence.entity.MonthlyBudgetEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface MonthlyBudgetJpaRepository : JpaRepository<MonthlyBudgetEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<MonthlyBudgetEntity>

    fun findAllByUserIdAndYear(userId: UUID, year: Int): List<MonthlyBudgetEntity>

    fun findByUserIdAndYearAndMonth(userId: UUID, year: Int, month: Int): MonthlyBudgetEntity?

    fun findByIdAndUserId(id: UUID, userId: UUID): MonthlyBudgetEntity?
}
