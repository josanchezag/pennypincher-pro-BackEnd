package com.pennypincher.pro.budget.infrastructure.web.controller

import com.pennypincher.pro.budget.infrastructure.persistence.repository.IncomeSourceJpaRepository
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

class IncomeSourceIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var sources: IncomeSourceJpaRepository

    @Test
    fun should_create_source_with_location_default_currency_and_two_decimal_amount() {
        val user = createUser()

        val result = create(user, """{"name":" AvenueCode ","category":"EMPLOYMENT","amount":15878607.36}""")
        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("AvenueCode") }
            jsonPath("$.data.category") { value("EMPLOYMENT") }
            jsonPath("$.data.currency") { value("COP") }
            jsonPath("$.data.createdAt") { isNotEmpty() }
            jsonPath("$.data.updatedAt") { isNotEmpty() }
            jsonPath("$.data.userId") { doesNotExist() }
            content { string(containsString("\"amount\":15878607.36")) }
        }

        val id = idOf(result)
        val location = checkNotNull(result.andReturn().response.getHeader(HttpHeaders.LOCATION))
        assertEquals("http://localhost$INCOME_SOURCES/$id", location)
        mockMvc.get(location) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.currency") { value("COP") }
        }
        val stored = sources.findById(id).orElseThrow()
        assertEquals(user.id, stored.userId)
        assertEquals("COP", stored.currency)
        assertTrue(stored.active)
    }

    @Test
    fun should_list_only_own_active_sources_sorted_by_category_then_name_and_filter_by_category() {
        val user = createUser()
        createSource(user, "Suasie", "BUSINESS")
        createSource(user, "Remoti", "EMPLOYMENT")
        createSource(user, "Airbnb Barranca", "RENTAL")
        createSource(user, "avenueCode", "EMPLOYMENT")
        deleteAs(user, "$INCOME_SOURCES/${createSource(user, "Vieja", "EMPLOYMENT")}")
        createSource(createUser(), "Ajena", "EMPLOYMENT")

        mockMvc.get(INCOME_SOURCES) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") { value(contains("avenueCode", "Remoti", "Airbnb Barranca", "Suasie")) }
        }
        mockMvc.get("$INCOME_SOURCES?category=EMPLOYMENT") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") { value(contains("avenueCode", "Remoti")) }
        }
    }

    @Test
    fun should_update_own_source_and_return_not_found_for_other_users_unknown_or_deleted_ids() {
        val owner = createUser()
        val id = createSource(owner, "Licor", "BUSINESS")
        val deleted = createSource(owner, "Vieja", "OTHER")
        deleteAs(owner, "$INCOME_SOURCES/$deleted")

        update(owner, id, """{"name":"Licores","category":"BUSINESS","amount":1500000.5,"currency":"USD"}""")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(id.toString()) }
                jsonPath("$.data.currency") { value("USD") }
                content { string(containsString("\"amount\":1500000.50")) }
            }
        val body = """{"name":"Robada","category":"OTHER","amount":1}"""
        listOf(createUser() to id, owner to UUID.randomUUID(), owner to deleted).forEach { (caller, target) ->
            update(caller, target, body).andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("NOT_FOUND") }
            }
        }
        assertEquals("Licores", sources.findById(id).orElseThrow().name)
    }

    @Test
    fun should_soft_delete_own_source_and_keep_it_when_another_user_tries() {
        val owner = createUser()
        val id = createSource(owner, "Arriendo Madera", "RENTAL")

        deleteAs(createUser(), "$INCOME_SOURCES/$id").andExpect { status { isNotFound() } }
        assertTrue(sources.findById(id).orElseThrow().active)

        deleteAs(owner, "$INCOME_SOURCES/$id").andExpect {
            status { isNoContent() }
            content { string("") }
        }
        assertFalse(sources.findById(id).orElseThrow().active)
        mockMvc.get("$INCOME_SOURCES/$id") { bearer(owner) }.andExpect { status { isNotFound() } }
    }

    @Test
    fun should_sum_only_own_active_amounts_when_requesting_total() {
        val user = createUser()
        mockMvc.get("$INCOME_SOURCES/total") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString("\"totalMonthlyIncome\":0.00")) }
        }
        createSource(user, "AvenueCode", "EMPLOYMENT", "15878607.36")
        createSource(user, "Remoti", "EMPLOYMENT", "21274315")
        deleteAs(user, "$INCOME_SOURCES/${createSource(user, "Vieja", "OTHER", "999")}")
        createSource(createUser(), "Ajena", "OTHER", "5")

        mockMvc.get("$INCOME_SOURCES/total") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString("\"totalMonthlyIncome\":37152922.36")) }
        }
    }

    @Test
    fun should_return_validation_error_when_currency_is_not_an_iso_code() {
        val user = createUser()

        create(user, """{"name":"Remoti","category":"EMPLOYMENT","amount":1,"currency":"usd"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_FAILED") }
            jsonPath("$.error.details[0].field") { value("currency") }
        }
    }

    private fun createSource(user: User, name: String, category: String, amount: String = "100"): UUID =
        idOf(create(user, """{"name":"$name","category":"$category","amount":$amount}"""))

    private fun create(user: User, body: String): ResultActionsDsl = postJson(user, INCOME_SOURCES, body)

    private fun update(user: User, id: UUID, body: String): ResultActionsDsl =
        putJson(user, "$INCOME_SOURCES/$id", body)

    private companion object {
        const val INCOME_SOURCES = "/api/v1/income-sources"
    }
}
