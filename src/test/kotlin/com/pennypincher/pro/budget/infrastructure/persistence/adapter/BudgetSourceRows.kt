package com.pennypincher.pro.budget.infrastructure.persistence.adapter

import java.util.UUID
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Inserts rows straight into the debt, expense and savings tables, which have no API yet, so that budget
 * tests can feed every component.
 */
class BudgetSourceRows(private val jdbc: JdbcTemplate) {
    fun creditCard(userId: UUID, monthlyPayment: String, active: Boolean = true) =
        insert(Row("credit_cards", "monthly_payment", "total_balance", "0"), userId, monthlyPayment, active)

    fun loan(userId: UUID, monthlyPayment: String, active: Boolean = true) =
        insert(Row("loans", "monthly_payment", "total_balance", "0"), userId, monthlyPayment, active)

    fun fixedExpense(userId: UUID, amount: String, category: String = "FOOD", active: Boolean = true) =
        insert(Row("fixed_expenses", "monthly_amount", "category", "'$category'"), userId, amount, active)

    fun variableExpense(userId: UUID, amount: String, active: Boolean = true) =
        insert(Row("variable_expenses", "monthly_amount", "category", "'LEISURE'"), userId, amount, active)

    fun savings(userId: UUID, contribution: String, active: Boolean = true) =
        insert(Row("savings_accounts", "monthly_contribution", "current_balance", "0"), userId, contribution, active)

    private fun insert(row: Row, userId: UUID, amount: String, active: Boolean) {
        jdbc.update(
            "INSERT INTO ${row.table} (user_id, name, ${row.amountColumn}, ${row.extraColumn}, is_active) " +
                "VALUES (?, 'Item', ?, ${row.extraValue}, ?)",
            userId,
            amount.toBigDecimal(),
            active,
        )
    }

    private data class Row(val table: String, val amountColumn: String, val extraColumn: String, val extraValue: String)
}
