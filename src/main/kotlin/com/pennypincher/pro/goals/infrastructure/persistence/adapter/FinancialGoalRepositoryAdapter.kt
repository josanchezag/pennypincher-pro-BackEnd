package com.pennypincher.pro.goals.infrastructure.persistence.adapter

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import com.pennypincher.pro.goals.domain.port.outbound.FinancialGoalRepository
import com.pennypincher.pro.goals.infrastructure.persistence.entity.FinancialGoalEntity
import com.pennypincher.pro.goals.infrastructure.persistence.repository.FinancialGoalJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Adapts Spring Data JPA to the domain [FinancialGoalRepository] port; deleting removes the row. */
@Component
@Transactional(readOnly = true)
class FinancialGoalRepositoryAdapter(private val jpaRepository: FinancialGoalJpaRepository) :
    FinancialGoalRepository {
    override fun findAllActive(userId: UUID, category: Nothing?): List<FinancialGoal> =
        jpaRepository.findAllByUserId(userId).map(::toDomain)

    override fun findActiveById(userId: UUID, id: UUID): FinancialGoal? =
        jpaRepository.findByIdAndUserId(id, userId)?.let(::toDomain)

    @Transactional
    override fun insert(item: FinancialGoal): FinancialGoal = toDomain(jpaRepository.save(toEntity(item)))

    /** A single `UPDATE` filtered by id and user, so a goal deleted after it was read is not inserted again. */
    @Transactional
    override fun update(item: FinancialGoal): FinancialGoal? =
        item.takeIf { jpaRepository.updateOwned(toEntity(item)) > 0 }

    /** Hard delete: the row is removed. */
    @Transactional
    override fun delete(userId: UUID, id: UUID): Boolean = jpaRepository.deleteOwned(id, userId) > 0

    private fun toDomain(entity: FinancialGoalEntity) = FinancialGoal(
        id = entity.id,
        userId = entity.userId,
        name = entity.name,
        level = GoalLevel.valueOf(entity.level),
        targetAmount = entity.targetAmount,
        monthlyInvestment = entity.monthlyInvestment,
        expectedAnnualReturn = entity.expectedAnnualReturn,
        targetYears = entity.targetYears,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    private fun toEntity(goal: FinancialGoal) = FinancialGoalEntity(
        id = goal.id,
        userId = goal.userId,
        name = goal.name,
        level = goal.level.name,
        targetAmount = goal.targetAmount,
        monthlyInvestment = goal.monthlyInvestment,
        expectedAnnualReturn = goal.expectedAnnualReturn,
        targetYears = goal.targetYears,
        createdAt = goal.createdAt,
        updatedAt = goal.updatedAt,
    )
}
