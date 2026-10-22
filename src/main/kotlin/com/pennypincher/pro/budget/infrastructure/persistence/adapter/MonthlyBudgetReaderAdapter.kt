package com.pennypincher.pro.budget.infrastructure.persistence.adapter

import com.pennypincher.pro.budget.domain.model.BudgetPeriod
import com.pennypincher.pro.budget.domain.model.MonthlyBudget
import com.pennypincher.pro.budget.domain.port.outbound.MonthlyBudgetReader
import com.pennypincher.pro.budget.infrastructure.persistence.repository.MonthlyBudgetJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Adapts Spring Data JPA to the domain [MonthlyBudgetReader] port. */
@Component
@Transactional(readOnly = true)
class MonthlyBudgetReaderAdapter(private val jpaRepository: MonthlyBudgetJpaRepository) : MonthlyBudgetReader {
    override fun findAll(userId: UUID, year: Int?): List<MonthlyBudget> {
        val entities = if (year == null) {
            jpaRepository.findAllByUserId(userId)
        } else {
            jpaRepository.findAllByUserIdAndYear(userId, year)
        }
        return entities.map(MonthlyBudgetEntityMapper::toDomain)
    }

    override fun findByPeriod(userId: UUID, period: BudgetPeriod): MonthlyBudget? =
        jpaRepository.findByUserIdAndYearAndMonth(userId, period.year, period.month)
            ?.let(MonthlyBudgetEntityMapper::toDomain)

    override fun findById(userId: UUID, id: UUID): MonthlyBudget? =
        jpaRepository.findByIdAndUserId(id, userId)?.let(MonthlyBudgetEntityMapper::toDomain)
}
