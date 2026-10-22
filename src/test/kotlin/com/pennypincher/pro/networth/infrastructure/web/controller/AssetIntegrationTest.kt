package com.pennypincher.pro.networth.infrastructure.web.controller

import com.pennypincher.pro.networth.infrastructure.persistence.repository.AssetJpaRepository
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get

class AssetIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var assets: AssetJpaRepository

    @Test
    fun should_create_asset_with_location_and_two_decimal_value_when_body_is_valid() {
        val user = createUser()

        val result = create(user, """{"name":"  Apto Madera ","value":350000000,"category":"REAL_ESTATE"}""")
        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Apto Madera") }
            jsonPath("$.data.category") { value("REAL_ESTATE") }
            jsonPath("$.data.createdAt") { isNotEmpty() }
            jsonPath("$.data.updatedAt") { isNotEmpty() }
            jsonPath("$.data.userId") { doesNotExist() }
            jsonPath("$.meta.requestId") { isNotEmpty() }
            content { string(containsString("\"value\":350000000.00")) }
        }

        val id = idOf(result)
        val location = result.andReturn().response.getHeader(HttpHeaders.LOCATION)
        assertEquals("http://localhost$ASSETS/$id", location)
        mockMvc.get(checkNotNull(location)) { bearer(user) }.andExpect { status { isOk() } }
        val stored = assets.findById(id).orElseThrow()
        assertEquals(user.id, stored.userId)
        assertTrue(stored.active)
    }

    @Test
    fun should_list_only_own_active_assets_sorted_by_category_then_name() {
        val user = createUser()
        createAsset(user, "Zeta", "OTHER")
        createAsset(user, "Carro Mazda", "VEHICLE")
        createAsset(user, "apto Madera", "REAL_ESTATE")
        createAsset(user, "Bicicleta", "VEHICLE")
        delete(user, createAsset(user, "Vendido", "VEHICLE"))
        createAsset(createUser(), "Ajeno", "REAL_ESTATE")

        mockMvc.get(ASSETS) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") { value(contains("apto Madera", "Bicicleta", "Carro Mazda", "Zeta")) }
            jsonPath("$.data[0].id") { isNotEmpty() }
            jsonPath("$.data[0].value") { isNumber() }
        }
    }

    @Test
    fun should_filter_by_category_when_query_parameter_is_given() {
        val user = createUser()
        createAsset(user, "Apto", "REAL_ESTATE")
        createAsset(user, "Carro", "VEHICLE")

        mockMvc.get("$ASSETS?category=REAL_ESTATE") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data[*].name") { value(contains("Apto")) }
        }
        mockMvc.get("$ASSETS?category=CASH") { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isEmpty() }
        }
    }

    @Test
    fun should_get_own_asset_by_id_and_return_not_found_for_other_users_or_deleted_assets() {
        val owner = createUser()
        val id = createAsset(owner, "Carro", "VEHICLE", "61000000")
        val deleted = createAsset(owner, "Moto", "VEHICLE")
        delete(owner, deleted)

        mockMvc.get("$ASSETS/$id") { bearer(owner) }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(id.toString()) }
            jsonPath("$.data.name") { value("Carro") }
            content { string(containsString("\"value\":61000000.00")) }
        }
        listOf(createUser() to id, owner to deleted, owner to UUID.randomUUID()).forEach { (caller, target) ->
            mockMvc.get("$ASSETS/$target") { bearer(caller) }.andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("NOT_FOUND") }
            }
        }
    }

    @Test
    fun should_update_own_asset_when_body_is_valid() {
        val user = createUser()
        val id = createAsset(user, "Carro", "VEHICLE")

        update(user, id, """{"name":"Carro Mazda 3","value":61000000.5,"category":"VEHICLE"}""").andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(id.toString()) }
            jsonPath("$.data.name") { value("Carro Mazda 3") }
            content { string(containsString("\"value\":61000000.50")) }
        }

        val stored = assets.findById(id).orElseThrow()
        assertEquals("Carro Mazda 3", stored.name)
        assertEquals("61000000.50", stored.amount.toPlainString())
    }

    @Test
    fun should_return_not_found_when_updating_asset_of_another_user_or_unknown_or_deleted() {
        val owner = createUser()
        val id = createAsset(owner, "Carro", "VEHICLE")
        val deleted = createAsset(owner, "Moto", "VEHICLE")
        delete(owner, deleted)
        val body = """{"name":"Robado","value":1,"category":"CASH"}"""

        listOf(createUser() to id, owner to UUID.randomUUID(), owner to deleted).forEach { (caller, target) ->
            update(caller, target, body).andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("NOT_FOUND") }
            }
        }
        assertEquals("Carro", assets.findById(id).orElseThrow().name)
    }

    @Test
    fun should_soft_delete_own_asset_and_return_no_content() {
        val user = createUser()
        val id = createAsset(user, "Carro", "VEHICLE")

        delete(user, id).andExpect {
            status { isNoContent() }
            content { string("") }
        }

        assertFalse(assets.findById(id).orElseThrow().active)
        delete(user, id).andExpect { status { isNotFound() } }
    }

    @Test
    fun should_return_not_found_and_keep_asset_when_another_user_deletes_it() {
        val id = createAsset(createUser(), "Carro", "VEHICLE")

        delete(createUser(), id).andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("NOT_FOUND") }
        }

        assertTrue(assets.findById(id).orElseThrow().active)
    }

    @Test
    fun should_sum_only_own_active_asset_values_when_requesting_total() {
        val user = createUser()
        mockMvc.get("$ASSETS/total") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString("\"totalAssets\":0.00")) }
        }
        createAsset(user, "Apto", "REAL_ESTATE", "350000000.25")
        createAsset(user, "Carro", "VEHICLE", "61000000")
        delete(user, createAsset(user, "Vendido", "VEHICLE", "999"))
        createAsset(createUser(), "Ajeno", "CASH", "5")

        mockMvc.get("$ASSETS/total") { bearer(user) }.andExpect {
            status { isOk() }
            content { string(containsString("\"totalAssets\":411000000.25")) }
        }
    }

    private fun createAsset(user: User, name: String, category: String, value: String = "100"): UUID =
        idOf(create(user, """{"name":"$name","value":$value,"category":"$category"}"""))

    private fun create(user: User, body: String): ResultActionsDsl = postJson(user, ASSETS, body)

    private fun update(user: User, id: UUID, body: String): ResultActionsDsl = putJson(user, "$ASSETS/$id", body)

    private fun delete(user: User, id: Any): ResultActionsDsl = deleteAs(user, "$ASSETS/$id")

    private companion object {
        const val ASSETS = "/api/v1/assets"
    }
}
