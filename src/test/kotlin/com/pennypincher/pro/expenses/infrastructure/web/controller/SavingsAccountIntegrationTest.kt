package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.infrastructure.persistence.repository.SavingsAccountJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.get

class SavingsAccountIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var accounts: SavingsAccountJpaRepository

    @Test
    fun should_return_created_account_with_location_and_two_decimals_when_posting() {
        val user = createUser()

        val result =
            postJson(
                user,
                SAVINGS,
                """{"name":" Licor Importado ","monthlyContribution":0,"currentBalance":15000000}""",
            )

        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Licor Importado") }
            jsonPath("$.data.userId") { doesNotExist() }
            content {
                string(containsString("\"monthlyContribution\":0.00"))
                string(containsString("\"currentBalance\":15000000.00"))
            }
        }
        val id = idOf(result)
        assertEquals("http://localhost$SAVINGS/$id", result.andReturn().response.getHeader(HttpHeaders.LOCATION))
    }

    @Test
    fun should_replace_fields_when_updating_and_soft_delete_when_deleting() {
        val user = createUser()
        val id = createAccount(user, "Licor Importado", "0", "15000000")

        putJson(
            user,
            "$SAVINGS/$id",
            """{"name":"Licor Importado","monthlyContribution":500000,"currentBalance":15500000.25}""",
        )
            .andExpect {
                status { isOk() }
                content { string(containsString("\"currentBalance\":15500000.25")) }
            }
        assertEquals("500000.00", accounts.findById(id).orElseThrow().monthlyAmount.toPlainString())
        deleteAs(user, "$SAVINGS/$id").andExpect { status { isNoContent() } }
        assertFalse(accounts.findById(id).orElseThrow().active)
    }

    @Test
    fun should_sort_by_name_and_sum_only_own_active_accounts_when_listing_and_summarizing() {
        val user = createUser()
        mockMvc.get("$SAVINGS/summary") { bearer(user) }.andExpect {
            content { string(containsString(summary("0.00", "0.00", 0))) }
        }
        createAccount(user, "Licor Importado", "0", "15000000")
        createAccount(user, "Colchon Licor", "0", "56000000")
        createAccount(user, "cash USD", "0", "5082000")
        createAccount(user, "Cuenta Facebank", "0", "0")
        deleteAs(user, "$SAVINGS/${createAccount(user, "Vieja", "999", "999")}")
        createAccount(createUser(), "Ajena", "5", "5")

        mockMvc.get(SAVINGS) { bearer(user) }.andExpect {
            jsonPath("$.data[*].name") {
                value(contains("cash USD", "Colchon Licor", "Cuenta Facebank", "Licor Importado"))
            }
        }
        mockMvc.get("$SAVINGS/summary") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString(summary("0.00", "76082000.00", 4))) }
        }
    }

    private fun summary(contributions: String, balance: String, count: Int) =
        """{"totalMonthlyContributions":$contributions,"totalCurrentBalance":$balance,"count":$count}"""

    private fun createAccount(user: User, name: String, contribution: String, balance: String): UUID = idOf(
        postJson(
            user,
            SAVINGS,
            """{"name":"$name","monthlyContribution":$contribution,"currentBalance":$balance}""",
        ),
    )

    private companion object {
        const val SAVINGS = "/api/v1/savings-accounts"
    }
}
