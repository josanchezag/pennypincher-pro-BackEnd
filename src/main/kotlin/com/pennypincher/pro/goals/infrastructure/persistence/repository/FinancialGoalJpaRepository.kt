package com.pennypincher.pro.goals.infrastructure.persistence.repository

import com.pennypincher.pro.goals.infrastructure.persistence.entity.FinancialGoalEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** Spring Data queries of the `financial_goals` table; all of them are scoped by user. */
interface FinancialGoalJpaRepository : JpaRepository<FinancialGoalEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<FinancialGoalEntity>

    fun findByIdAndUserId(id: UUID, userId: UUID): FinancialGoalEntity?

    /** Writes the editable columns of [goal] to its row of the same user; 0 when that row no longer exists. */
    @Modifying
    @Query(
        "update FinancialGoalEntity g set g.name = :#{#goal.name}, g.level = :#{#goal.level}, " +
            "g.targetAmount = :#{#goal.targetAmount}, g.monthlyInvestment = :#{#goal.monthlyInvestment}, " +
            "g.expectedAnnualReturn = :#{#goal.expectedAnnualReturn}, g.targetYears = :#{#goal.targetYears}, " +
            "g.updatedAt = :#{#goal.updatedAt} where g.id = :#{#goal.id} and g.userId = :#{#goal.userId}",
    )
    fun updateOwned(@Param("goal") goal: FinancialGoalEntity): Int

    @Modifying
    @Query("delete from FinancialGoalEntity g where g.id = :id and g.userId = :userId")
    fun deleteOwned(@Param("id") id: UUID, @Param("userId") userId: UUID): Int
}
