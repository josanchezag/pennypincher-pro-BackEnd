package com.pennypincher.pro.budget.infrastructure.persistence.adapter

import com.pennypincher.pro.budget.domain.model.BudgetSourceAmounts
import com.pennypincher.pro.budget.domain.port.outbound.BudgetSourceTotals
import java.sql.ResultSet
import java.util.UUID
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/**
 * Adapts the income, debt, expense and savings tables to the [BudgetSourceTotals] port. All the sums come
 * from one SQL statement, so PostgreSQL computes them over the same snapshot. Only active rows of the user
 * count; the tables other than `income_sources` belong to the expenses module and are only summed here.
 */
@Component
class BudgetSourceTotalsAdapter(private val jdbcTemplate: NamedParameterJdbcTemplate) : BudgetSourceTotals {
    override fun forUser(userId: UUID): BudgetSourceAmounts {
        val parameters = MapSqlParameterSource(USER_ID, userId).addValue(CHARITY, CHARITY_CATEGORY)
        return checkNotNull(jdbcTemplate.queryForObject(TOTALS_SQL, parameters) { row, _ -> toAmounts(row) })
    }

    private fun toAmounts(row: ResultSet) = BudgetSourceAmounts(
        totalIncome = row.getBigDecimal("total_income"),
        creditCardPayments = row.getBigDecimal("credit_card_payments"),
        loanPayments = row.getBigDecimal("loan_payments"),
        nonCharityFixedExpenses = row.getBigDecimal("non_charity_fixed_expenses"),
        charityFixedExpenses = row.getBigDecimal("charity_fixed_expenses"),
        variableExpenses = row.getBigDecimal("variable_expenses"),
        savingsContributions = row.getBigDecimal("savings_contributions"),
    )

    private companion object {
        const val USER_ID = "userId"
        const val CHARITY = "charity"
        const val CHARITY_CATEGORY = "CHARITY"
        const val ACTIVE_ROWS = "WHERE user_id = :$USER_ID AND is_active"
        val TOTALS_SQL =
            """
            SELECT
                (SELECT COALESCE(SUM(amount), 0) FROM income_sources $ACTIVE_ROWS) AS total_income,
                (SELECT COALESCE(SUM(monthly_payment), 0) FROM credit_cards $ACTIVE_ROWS) AS credit_card_payments,
                (SELECT COALESCE(SUM(monthly_payment), 0) FROM loans $ACTIVE_ROWS) AS loan_payments,
                (SELECT COALESCE(SUM(monthly_amount), 0) FROM fixed_expenses $ACTIVE_ROWS AND category <> :$CHARITY)
                    AS non_charity_fixed_expenses,
                (SELECT COALESCE(SUM(monthly_amount), 0) FROM fixed_expenses $ACTIVE_ROWS AND category = :$CHARITY)
                    AS charity_fixed_expenses,
                (SELECT COALESCE(SUM(monthly_amount), 0) FROM variable_expenses $ACTIVE_ROWS) AS variable_expenses,
                (SELECT COALESCE(SUM(monthly_contribution), 0) FROM savings_accounts $ACTIVE_ROWS)
                    AS savings_contributions
            """.trimIndent()
    }
}
