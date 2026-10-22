package com.pennypincher.pro.budget.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Amount and share of the income (4 decimals) of each budget component. */
@Embeddable
class BudgetComponentColumns(
    @Column(name = "credit_card_total", nullable = false)
    var creditCardTotal: BigDecimal,
    @Column(name = "credit_card_percentage", nullable = false)
    var creditCardPercentage: BigDecimal,
    @Column(name = "loan_total", nullable = false)
    var loanTotal: BigDecimal,
    @Column(name = "loan_percentage", nullable = false)
    var loanPercentage: BigDecimal,
    @Column(name = "expense_total", nullable = false)
    var expenseTotal: BigDecimal,
    @Column(name = "expense_percentage", nullable = false)
    var expensePercentage: BigDecimal,
    @Column(name = "savings_total", nullable = false)
    var savingsTotal: BigDecimal,
    @Column(name = "savings_percentage", nullable = false)
    var savingsPercentage: BigDecimal,
    @Column(name = "charity_total", nullable = false)
    var charityTotal: BigDecimal,
    @Column(name = "charity_percentage", nullable = false)
    var charityPercentage: BigDecimal,
)

/** JPA mapping of the `monthly_budgets` table (one row per user and month). */
@Entity
@Table(name = "monthly_budgets")
class MonthlyBudgetEntity(
    @Id
    val id: UUID,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "year", nullable = false, updatable = false)
    val year: Int,
    @Column(name = "month", nullable = false, updatable = false)
    val month: Int,
    @Column(name = "exchange_rate")
    var exchangeRate: BigDecimal?,
    @Column(name = "work_days")
    var workDays: Int?,
    @Column(name = "total_income", nullable = false)
    var totalIncome: BigDecimal,
    @Column(name = "total_expenses", nullable = false)
    var totalExpenses: BigDecimal,
    @Column(name = "monthly_cash_flow", nullable = false)
    var monthlyCashFlow: BigDecimal,
    @Embedded
    var components: BudgetComponentColumns,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
