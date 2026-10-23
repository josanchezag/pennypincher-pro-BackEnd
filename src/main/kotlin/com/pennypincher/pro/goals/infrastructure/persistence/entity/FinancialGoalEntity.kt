package com.pennypincher.pro.goals.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** JPA mapping of the `financial_goals` table. There is no `is_active` column: goals are hard deleted. */
@Entity
@Table(name = "financial_goals")
class FinancialGoalEntity(
    @Id
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "level", nullable = false)
    var level: String,
    @Column(name = "target_amount", nullable = false)
    var targetAmount: BigDecimal,
    @Column(name = "monthly_investment", nullable = false)
    var monthlyInvestment: BigDecimal,
    @Column(name = "expected_annual_return", nullable = false)
    var expectedAnnualReturn: BigDecimal,
    @Column(name = "target_years")
    var targetYears: Int?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
