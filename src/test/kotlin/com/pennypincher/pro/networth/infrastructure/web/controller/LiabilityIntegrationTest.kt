package com.pennypincher.pro.networth.infrastructure.web.controller

import com.pennypincher.pro.networth.infrastructure.persistence.repository.LiabilityJpaRepository
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

class LiabilityIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var liabilities: LiabilityJpaRepository

    @Test
    fun should_create_liability_with_location_and_two_decimal_balance_when_body_is_valid() {
        val user = createUser()

        val result = create(user, """{"name":" Tarjeta Nu Bank ","currentBalance":2500000,"category":"CREDIT_CARD"}""")
        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Tarjeta Nu Bank") }
            jsonPath("$.data.category") { value("CREDIT_CARD") }
            jsonPath("$.data.createdAt") { isNotEmpty() }
            jsonPath("$.data.updatedAt") { isNotEmpty() }
            jsonPath("$.data.userId") { doesNotExist() }
            content { string(containsString("\"currentBalance\":2500000.00")) }
        }

        val id = idOf(result)
        val location = checkNotNull(result.andReturn().response.getHeader(HttpHeaders.LOCATION))
        assertEquals("http://localhost$LIABILITIES/$id", location)
        mockMvc.get(location) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(id.toString()) }
            jsonPath("$.data.name") { value("Tarjeta Nu Bank") }
        }
        assertEquals(user.id, liabilities.findById(id).orElseThrow().userId)
    }

    @Test
    fun should_list_only_own_active_liabilities_sorted_by_category_then_name() {
        val user = createUser()
        createLiability(user, "Tío Pedro", "FAMILY_DEBT")
        createLiability(user, "visa", "CREDIT_CARD")
        createLiability(user, "Davivienda 7141", "BANK_LOAN")
        createLiability(user, "Nu Bank", "CREDIT_CARD")
        delete(user, createLiability(user, "Pagada", "CREDIT_CARD"))
        createLiability(createUser(), "Ajena", "CREDIT_CARD")

        mockMvc.get(LIABILITIES) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") { value(contains("Nu Bank", "visa", "Davivienda 7141", "Tío Pedro")) }
        }
        mockMvc.get("$LIABILITIES?category=CREDIT_CARD") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") { value(contains("Nu Bank", "visa")) }
        }
    }

    @Test
    fun should_update_own_liability_and_return_not_found_for_other_users() {
        val owner = createUser()
        val id = createLiability(owner, "Visa", "CREDIT_CARD")
        val body = """{"name":"Visa Oro","currentBalance":0,"category":"CREDIT_CARD"}"""

        update(createUser(), id, body).andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("NOT_FOUND") }
        }
        update(owner, UUID.randomUUID(), body).andExpect { status { isNotFound() } }
        update(owner, id, body).andExpect {
            status { isOk() }
            jsonPath("$.data.name") { value("Visa Oro") }
            content { string(containsString("\"currentBalance\":0.00")) }
        }
        assertEquals("Visa Oro", liabilities.findById(id).orElseThrow().name)
    }

    @Test
    fun should_soft_delete_own_liability_and_reject_deletion_by_other_users() {
        val owner = createUser()
        val id = createLiability(owner, "Visa", "CREDIT_CARD")

        val stranger = createUser()
        delete(stranger, id).andExpect { status { isNotFound() } }
        mockMvc.get("$LIABILITIES/$id") { bearer(stranger) }.andExpect { status { isNotFound() } }
        assertTrue(liabilities.findById(id).orElseThrow().active)

        delete(owner, id).andExpect { status { isNoContent() } }
        assertFalse(liabilities.findById(id).orElseThrow().active)
        delete(owner, id).andExpect { status { isNotFound() } }
        mockMvc.get("$LIABILITIES/$id") { bearer(owner) }.andExpect { status { isNotFound() } }
        update(owner, id, """{"name":"Visa","currentBalance":1,"category":"OTHER"}""").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun should_sum_only_own_active_balances_when_requesting_total() {
        val user = createUser()
        mockMvc.get("$LIABILITIES/total") { bearer(user) }.andExpect {
            content { string(containsString("\"totalLiabilities\":0.00")) }
        }
        createLiability(user, "Visa", "CREDIT_CARD", "2500000.75")
        createLiability(user, "Davivienda", "BANK_LOAN", "40000000")
        delete(user, createLiability(user, "Pagada", "OTHER", "999"))
        createLiability(createUser(), "Ajena", "OTHER", "7")

        mockMvc.get("$LIABILITIES/total") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString("\"totalLiabilities\":42500000.75")) }
        }
    }

    private fun createLiability(user: User, name: String, category: String, balance: String = "100"): UUID =
        idOf(create(user, """{"name":"$name","currentBalance":$balance,"category":"$category"}"""))

    private fun create(user: User, body: String): ResultActionsDsl = postJson(user, LIABILITIES, body)

    private fun update(user: User, id: UUID, body: String): ResultActionsDsl = putJson(user, "$LIABILITIES/$id", body)

    private fun delete(user: User, id: UUID): ResultActionsDsl = deleteAs(user, "$LIABILITIES/$id")

    private companion object {
        const val LIABILITIES = "/api/v1/liabilities"
    }
}
