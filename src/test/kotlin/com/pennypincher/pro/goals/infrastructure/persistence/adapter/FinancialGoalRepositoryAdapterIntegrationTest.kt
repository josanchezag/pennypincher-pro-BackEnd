package com.pennypincher.pro.goals.infrastructure.persistence.adapter

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import com.pennypincher.pro.goals.domain.port.outbound.FinancialGoalRepository
import com.pennypincher.pro.goals.infrastructure.persistence.repository.FinancialGoalJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FinancialGoalRepositoryAdapterIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var repository: FinancialGoalRepository

    @Autowired
    private lateinit var goals: FinancialGoalJpaRepository

    @Test
    fun should_not_insert_goal_again_when_an_update_read_before_the_delete_is_saved_after_it() {
        val userId = createUser().id
        val saved = repository.insert(goal(userId))
        val readBeforeDelete = checkNotNull(repository.findActiveById(userId, saved.id))

        assertTrue(repository.delete(userId, saved.id))

        assertNull(repository.update(readBeforeDelete.copy(name = "Late update")))
        assertFalse(goals.existsById(saved.id))
    }

    @Test
    fun should_update_only_the_owners_row() {
        val userId = createUser().id
        val saved = repository.insert(goal(userId))
        val changed = saved.copy(name = "Libertad", level = GoalLevel.FINANCIAL_FREEDOM, targetYears = null)

        assertNull(repository.update(changed.copy(userId = createUser().id)))
        assertEquals("Seguridad", goals.findById(saved.id).orElseThrow().name)
        assertEquals(changed, repository.update(changed))
        val stored = goals.findById(saved.id).orElseThrow()
        assertEquals("Libertad", stored.name)
        assertEquals("FINANCIAL_FREEDOM", stored.level)
        assertNull(stored.targetYears)
    }

    private fun goal(userId: UUID) = FinancialGoal(
        id = UUID.randomUUID(),
        userId = userId,
        name = "Seguridad",
        level = GoalLevel.FINANCIAL_SECURITY,
        targetAmount = BigDecimal("605000000.00"),
        monthlyInvestment = BigDecimal("1470000.00"),
        expectedAnnualReturn = BigDecimal("0.1100"),
        targetYears = TARGET_YEARS,
        createdAt = NOW,
        updatedAt = NOW,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-23T00:00:00Z")
        const val TARGET_YEARS = 16
    }
}
