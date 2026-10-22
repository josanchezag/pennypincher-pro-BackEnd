package com.pennypincher.pro.budget.infrastructure.persistence.adapter

import com.pennypincher.pro.budget.domain.model.MonthlyBudget
import com.pennypincher.pro.budget.domain.port.outbound.MonthlyBudgetWriter
import com.pennypincher.pro.budget.infrastructure.persistence.entity.MonthlyBudgetEntity
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Adapts the `monthly_budgets` table to the domain [MonthlyBudgetWriter] port with single PostgreSQL
 * statements: `INSERT ... ON CONFLICT DO NOTHING`, so concurrent inserts of one month never fail, and
 * `UPDATE ... RETURNING updated_at`, so the caller gets the timestamp the trigger stored.
 */
@Component
@Transactional
class MonthlyBudgetWriterAdapter(private val jdbcTemplate: NamedParameterJdbcTemplate) : MonthlyBudgetWriter {
    override fun insertIfAbsent(budget: MonthlyBudget): Boolean =
        jdbcTemplate.update(INSERT_SQL, parametersOf(budget)) > 0

    override fun update(budget: MonthlyBudget): MonthlyBudget? {
        val stored = jdbcTemplate.query(UPDATE_SQL, parametersOf(budget)) { row, _ ->
            row.getObject(UPDATED_AT, OffsetDateTime::class.java).toInstant()
        }
        return stored.singleOrNull()?.let { budget.copy(updatedAt = it) }
    }

    private fun parametersOf(budget: MonthlyBudget): MapSqlParameterSource {
        val entity: MonthlyBudgetEntity = MonthlyBudgetEntityMapper.toEntity(budget)
        val components = entity.components
        return MapSqlParameterSource()
            .addValue("id", entity.id)
            .addValue("userId", entity.userId)
            .addValue("year", entity.year)
            .addValue("month", entity.month)
            .addValue("exchangeRate", entity.exchangeRate)
            .addValue("workDays", entity.workDays)
            .addValue("totalIncome", entity.totalIncome)
            .addValue("totalExpenses", entity.totalExpenses)
            .addValue("monthlyCashFlow", entity.monthlyCashFlow)
            .addValue("creditCardTotal", components.creditCardTotal)
            .addValue("creditCardPercentage", components.creditCardPercentage)
            .addValue("loanTotal", components.loanTotal)
            .addValue("loanPercentage", components.loanPercentage)
            .addValue("expenseTotal", components.expenseTotal)
            .addValue("expensePercentage", components.expensePercentage)
            .addValue("savingsTotal", components.savingsTotal)
            .addValue("savingsPercentage", components.savingsPercentage)
            .addValue("charityTotal", components.charityTotal)
            .addValue("charityPercentage", components.charityPercentage)
            .addValue("createdAt", utc(entity.createdAt))
            .addValue("updatedAt", utc(entity.updatedAt))
    }

    private fun utc(instant: Instant): OffsetDateTime = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)

    private companion object {
        const val UPDATED_AT = "updated_at"
        val INSERT_SQL =
            """
            INSERT INTO monthly_budgets
                (id, user_id, year, month, exchange_rate, work_days, total_income, total_expenses, monthly_cash_flow,
                 credit_card_total, credit_card_percentage, loan_total, loan_percentage, expense_total,
                 expense_percentage, savings_total, savings_percentage, charity_total, charity_percentage,
                 created_at, updated_at)
            VALUES (:id, :userId, :year, :month, :exchangeRate, :workDays, :totalIncome, :totalExpenses,
                    :monthlyCashFlow, :creditCardTotal, :creditCardPercentage, :loanTotal, :loanPercentage,
                    :expenseTotal, :expensePercentage, :savingsTotal, :savingsPercentage, :charityTotal,
                    :charityPercentage, :createdAt, :updatedAt)
            ON CONFLICT ON CONSTRAINT uq_monthly_budgets_user_period DO NOTHING
            """.trimIndent()
        val UPDATE_SQL =
            """
            UPDATE monthly_budgets SET
                exchange_rate = :exchangeRate, work_days = :workDays, total_income = :totalIncome,
                total_expenses = :totalExpenses, monthly_cash_flow = :monthlyCashFlow,
                credit_card_total = :creditCardTotal, credit_card_percentage = :creditCardPercentage,
                loan_total = :loanTotal, loan_percentage = :loanPercentage,
                expense_total = :expenseTotal, expense_percentage = :expensePercentage,
                savings_total = :savingsTotal, savings_percentage = :savingsPercentage,
                charity_total = :charityTotal, charity_percentage = :charityPercentage, updated_at = :updatedAt
            WHERE id = :id AND user_id = :userId
            RETURNING $UPDATED_AT
            """.trimIndent()
    }
}
