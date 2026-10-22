package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.infrastructure.persistence.repository.CreditCardJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get

class CreditCardIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var cards: CreditCardJpaRepository

    @Test
    fun should_return_created_card_with_location_trimmed_name_and_two_decimals_when_posting() {
        val user = createUser()

        val result =
            postJson(user, CARDS, """{"name":" Tarjeta Nu Bank ","monthlyPayment":0,"totalBalance":12407700}""")
        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Tarjeta Nu Bank") }
            jsonPath("$.data.createdAt") { isNotEmpty() }
            jsonPath("$.data.updatedAt") { isNotEmpty() }
            jsonPath("$.data.userId") { doesNotExist() }
            content {
                string(containsString("\"monthlyPayment\":0.00"))
                string(containsString("\"totalBalance\":12407700.00"))
            }
        }

        expectStoredAtLocation(user, result)
    }

    @Test
    fun should_list_only_own_active_cards_sorted_by_name_when_listing() {
        val user = createUser()
        createCard(user, "Tarjeta Nu Bank")
        createCard(user, "addi Yasmin")
        createCard(user, "Addi Josi")
        deleteAs(user, "$CARDS/${createCard(user, "Vieja")}")
        createCard(createUser(), "Ajena")

        mockMvc.get(CARDS) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") { value(contains("Addi Josi", "addi Yasmin", "Tarjeta Nu Bank")) }
        }
    }

    @Test
    fun should_replace_fields_when_updating_and_soft_delete_when_deleting() {
        val user = createUser()
        val id = createCard(user, "Occidente")

        putJson(
            user,
            "$CARDS/$id",
            """{"name":"Banco de Occidente","monthlyPayment":350000.5,"totalBalance":30049222}""",
        )
            .andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(id.toString()) }
                jsonPath("$.data.name") { value("Banco de Occidente") }
                content { string(containsString("\"monthlyPayment\":350000.50")) }
            }
        deleteAs(user, "$CARDS/$id").andExpect {
            status { isNoContent() }
            content { string("") }
        }
        assertFalse(cards.findById(id).orElseThrow().active)
    }

    @Test
    fun should_sum_only_own_active_cards_when_requesting_summary() {
        val user = createUser()
        mockMvc.get("$CARDS/summary") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString(summary("0.00", "0.00", 0))) }
        }
        createCard(user, "Tarjeta Nu Bank", "0", "12407700")
        createCard(user, "Tarjeta Banco de Occidente", "0", "30049222")
        createCard(user, "Tarjeta Nu Bank Yasmin", "0", "500000")
        deleteAs(user, "$CARDS/${createCard(user, "Vieja", "999", "999")}")
        createCard(createUser(), "Ajena", "5", "5")

        mockMvc.get("$CARDS/summary") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString(summary("0.00", "42956922.00", 3))) }
        }
    }

    private fun expectStoredAtLocation(user: User, result: ResultActionsDsl) {
        val id = idOf(result)
        val location = checkNotNull(result.andReturn().response.getHeader(HttpHeaders.LOCATION))
        assertEquals("http://localhost$CARDS/$id", location)
        mockMvc.get(location) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(id.toString()) }
        }
        val stored = cards.findById(id).orElseThrow()
        assertEquals(user.id, stored.userId)
        assertEquals("12407700.00", stored.balance.toPlainString())
        assertTrue(stored.active)
    }

    private fun summary(payments: String, balance: String, count: Int) =
        """{"totalMonthlyPayments":$payments,"totalOutstandingBalance":$balance,"count":$count}"""

    private fun createCard(user: User, name: String, payment: String = "100", balance: String = "1000"): UUID =
        idOf(postJson(user, CARDS, """{"name":"$name","monthlyPayment":$payment,"totalBalance":$balance}"""))

    private companion object {
        const val CARDS = "/api/v1/credit-cards"
    }
}
