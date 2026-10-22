package com.pennypincher.pro.budget.infrastructure.web.controller

import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.util.UUID
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * End to end: the spreadsheet's expense details entered through the expense APIs feed the monthly budget,
 * which only changes when it is created, updated or recalculated.
 */
class ExpensesBudgetIntegrationTest : ApiIntegrationTest() {
    @Test
    fun should_build_budget_components_from_active_expense_details_when_creating() {
        val user = createUser()
        seedSpreadsheet(user)

        val result = postJson(user, BUDGETS, """{"year":2026,"month":10}""")

        result.andExpect { status { isCreated() } }
        expectFigures(result, SPREADSHEET_FIGURES)
    }

    @Test
    fun should_pick_up_expense_changes_only_when_recalculating() {
        val user = createUser()
        val seeded = seedSpreadsheet(user)
        val id = idOf(postJson(user, BUDGETS, """{"year":2026,"month":10}"""))

        putJson(
            user,
            "$CARDS/${seeded.card}",
            """{"name":"Tarjeta Nu Bank","monthlyPayment":500000,"totalBalance":12407700}""",
        )
        putJson(
            user,
            "$SAVINGS/${seeded.savings}",
            """{"name":"Licor Importado","monthlyContribution":1000000,"currentBalance":0}""",
        )
        deleteAs(user, "$FIXED/${seeded.fixed}").andExpect { status { isNoContent() } }

        val stored = mockMvc.get("$BUDGETS/2026/10") { bearer(user) }
        expectFigures(stored, SPREADSHEET_FIGURES)
        val recalculated = mockMvc.post("$BUDGETS/$id/recalculate") { bearer(user) }
        recalculated.andExpect {
            status { isOk() }
            content { string(containsString("\"totalExpenses\":19185201.00")) }
            content { string(containsString("\"monthlyCashFlow\":17967721.36")) }
        }
        expectFigures(recalculated, Figures("500000.00", "1.35", "15354970.00", "41.33", "1000000.00", "2.69"))
    }

    private fun expectFigures(result: ResultActionsDsl, figures: Figures) = result.andExpect {
        content {
            string(containsString("\"totalIncome\":37152922.36"))
            string(containsString(component("CREDIT_CARDS", figures.cards, figures.cardsPercentage, "20.00")))
            string(containsString(component("LOANS", "3280231.00", "8.83", "null")))
            string(containsString(component("MONTHLY_EXPENSES", figures.expenses, figures.expensesPercentage, "null")))
            string(containsString(component("SAVINGS", figures.savings, figures.savingsPercentage, "10.00")))
            string(containsString(component("CHARITY", "50000.00", "0.13", "10.00")))
        }
    }

    /** Creates the spreadsheet month through the APIs, plus deleted and foreign rows that must not count. */
    private fun seedSpreadsheet(user: User): Seeded {
        create(user, INCOME, """{"name":"AvenueCode","category":"EMPLOYMENT","amount":15878607.36}""")
        create(user, INCOME, """{"name":"Remoti","category":"EMPLOYMENT","amount":21274315}""")
        val card = CREDIT_CARDS.map { (name, balance) -> createBalance(user, CARDS, DEBT_FIELDS, name, "0", balance) }
        LOANS_SHEET.forEach { (name, payment, balance) ->
            createBalance(user, LOANS, DEBT_FIELDS, name, payment, balance)
        }
        val fixed = FIXED_EXPENSES.map { (name, amount, category) ->
            createExpense(user, FIXED, name, amount, category)
        }
        VARIABLE_EXPENSES.forEach { (name, amount, category) -> createExpense(user, VARIABLE, name, amount, category) }
        val savings = SAVINGS_SHEET.map { (name, balance) ->
            createBalance(user, SAVINGS, SAVINGS_FIELDS, name, "0", balance)
        }
        seedDeletedAndForeignRows(user)
        return Seeded(card = card.first(), fixed = fixed.first(), savings = savings.first())
    }

    private fun seedDeletedAndForeignRows(user: User) {
        deleteAs(user, "$CARDS/${createBalance(user, CARDS, DEBT_FIELDS, "Vieja", "999999", "1")}")
        deleteAs(user, "$LOANS/${createBalance(user, LOANS, DEBT_FIELDS, "Pagado", "999999", "1")}")
        deleteAs(user, "$FIXED/${createExpense(user, FIXED, "Donación vieja", "999999", "CHARITY")}")
        deleteAs(user, "$VARIABLE/${createExpense(user, VARIABLE, "Gimnasio", "999999", "LEISURE")}")
        deleteAs(user, "$SAVINGS/${createBalance(user, SAVINGS, SAVINGS_FIELDS, "Cerrada", "999999", "1")}")
        createExpense(createUser(), FIXED, "Ajena", "999999", "CHARITY")
    }

    private fun createBalance(
        user: User,
        path: String,
        fields: Pair<String, String>,
        name: String,
        monthly: String,
        balance: String,
    ): UUID = create(user, path, """{"name":"$name","${fields.first}":$monthly,"${fields.second}":$balance}""")

    private fun createExpense(user: User, path: String, name: String, amount: String, category: String): UUID =
        create(user, path, """{"name":"$name","monthlyAmount":$amount,"category":"$category"}""")

    private fun create(user: User, path: String, body: String): UUID =
        idOf(postJson(user, path, body).andExpect { status { isCreated() } })

    private fun component(key: String, amount: String, percentage: String, ideal: String) =
        """{"component":"$key","amount":$amount,"percentage":$percentage,"idealPercentage":$ideal}"""

    /** The component figures that change in these tests; loans, charity and income stay fixed. */
    private data class Figures(
        val cards: String,
        val cardsPercentage: String,
        val expenses: String,
        val expensesPercentage: String,
        val savings: String,
        val savingsPercentage: String,
    )

    /** Ids the recalculation test edits: the first card, the first fixed expense and the first savings account. */
    private data class Seeded(val card: UUID, val fixed: UUID, val savings: UUID)

    private companion object {
        const val BUDGETS = "/api/v1/monthly-budgets"
        const val INCOME = "/api/v1/income-sources"
        const val CARDS = "/api/v1/credit-cards"
        const val LOANS = "/api/v1/loans"
        const val FIXED = "/api/v1/fixed-expenses"
        const val VARIABLE = "/api/v1/variable-expenses"
        const val SAVINGS = "/api/v1/savings-accounts"
        val DEBT_FIELDS = "monthlyPayment" to "totalBalance"
        val SAVINGS_FIELDS = "monthlyContribution" to "currentBalance"
        val SPREADSHEET_FIGURES = Figures("0.00", "0.00", "15804970.00", "42.54", "0.00", "0.00")

        val CREDIT_CARDS = listOf(
            "Tarjeta Nu Bank" to "12407700",
            "Tarjeta Banco de Occidente" to "30049222",
            "Tarjeta Nu Bank Yasmin" to "500000",
            "Addi Josi" to "0",
            "Addi Yasmin" to "0",
        )
        val LOANS_SHEET = listOf(
            Triple("Prestamo Occidente 190028884", "1258908", "88614841"),
            Triple("Prestamo Davivienda 7141", "1339823", "59781329"),
            Triple("Prestamo Davivienda 2670", "681500", "22727489"),
        )
        val FIXED_EXPENSES = listOf(
            Triple("Gasolina Carro", "450000", "TRANSPORT"),
            Triple("Mercado", "1100000", "FOOD"),
            Triple("Tigo San Javier", "107000", "COMMUNICATION"),
            Triple("EPM", "333100", "UTILITIES"),
            Triple("Poliza Autos", "321388", "INSURANCE"),
            Triple("Colegio Thiana", "2271042", "EDUCATION"),
            Triple("Donación Aldeas Colombia", "50000", "CHARITY"),
            Triple("Celular Josimar", "44600", "COMMUNICATION"),
            Triple("Celular Yasmin", "41000", "COMMUNICATION"),
            Triple("Arriendo San Javier", "2543000", "HOUSING"),
            Triple("Plan Dental Axa", "0", "HEALTH"),
            Triple("Retencion en la fuente", "1764000", "TAXES"),
            Triple("Descuentos S&P", "1620000", "OTHER"),
            Triple("Poliza Salud", "1382421", "INSURANCE"),
            Triple("Impuesto Carro", "147000", "TAXES"),
            Triple("SOAT", "80124", "INSURANCE"),
            Triple("Tecnomecanica", "26208", "TRANSPORT"),
            Triple("Natación Thiana", "76667", "EDUCATION"),
            Triple("Clases BioRepro Yasmin", "1000000", "HEALTH"),
        )
        val VARIABLE_EXPENSES = listOf(
            Triple("Ocio Josi", "500000", "LEISURE"),
            Triple("Ocio Yas", "1000000", "LEISURE"),
            Triple("Ocio Casa", "500000", "LEISURE"),
            Triple("Spotify", "0", "STREAMING"),
            Triple("Netflix", "42000", "STREAMING"),
            Triple("DisneyPlus", "47420", "STREAMING"),
            Triple("Doña Gladys", "360000", "HOUSEHOLD_STAFF"),
            Triple("Motilada", "48000", "HOUSEHOLD_STAFF"),
        )
        val SAVINGS_SHEET = listOf(
            "Licor Importado" to "15000000",
            "Colchon Licor" to "56000000",
            "Cash USD" to "5082000",
            "Cuenta Facebank" to "0",
        )
    }
}
