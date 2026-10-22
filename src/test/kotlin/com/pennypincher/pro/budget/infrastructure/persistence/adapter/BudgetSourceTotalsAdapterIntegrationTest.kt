package com.pennypincher.pro.budget.infrastructure.persistence.adapter

import com.pennypincher.pro.budget.domain.port.outbound.BudgetSourceTotals
import com.pennypincher.pro.support.ApiIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class BudgetSourceTotalsAdapterIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var totals: BudgetSourceTotals

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun should_return_zero_for_every_sum_when_user_has_no_rows() {
        val amounts = totals.forUser(createUser().id)

        listOf(
            amounts.totalIncome,
            amounts.creditCardPayments,
            amounts.loanPayments,
            amounts.nonCharityFixedExpenses,
            amounts.charityFixedExpenses,
            amounts.variableExpenses,
            amounts.savingsContributions,
        ).forEach { assertEquals(0, it.signum()) }
    }

    @Test
    fun should_sum_only_active_rows_of_the_user_and_split_charity_fixed_expenses() {
        val user = createUser()
        val userId = user.id
        val otherId = createUser().id
        postJson(user, "/api/v1/income-sources", """{"name":"Sueldo","category":"EMPLOYMENT","amount":1000.5}""")
        val rows = BudgetSourceRows(jdbc)
        rows.creditCard(userId, "100.10")
        rows.creditCard(userId, "50.00")
        rows.creditCard(userId, "999.00", active = false)
        rows.creditCard(otherId, "999.00")
        rows.loan(userId, "200.00")
        rows.fixedExpense(userId, "300.00", "FOOD")
        rows.fixedExpense(userId, "30.00", "HOUSING")
        rows.fixedExpense(userId, "40.00", "CHARITY")
        rows.fixedExpense(userId, "999.00", "CHARITY", active = false)
        rows.variableExpense(userId, "60.00")
        rows.savings(userId, "70.00")

        val amounts = totals.forUser(userId)

        assertEquals("1000.50", amounts.totalIncome.toPlainString())
        assertEquals("150.10", amounts.creditCardPayments.toPlainString())
        assertEquals("200.00", amounts.loanPayments.toPlainString())
        assertEquals("330.00", amounts.nonCharityFixedExpenses.toPlainString())
        assertEquals("40.00", amounts.charityFixedExpenses.toPlainString())
        assertEquals("60.00", amounts.variableExpenses.toPlainString())
        assertEquals("70.00", amounts.savingsContributions.toPlainString())
    }
}
