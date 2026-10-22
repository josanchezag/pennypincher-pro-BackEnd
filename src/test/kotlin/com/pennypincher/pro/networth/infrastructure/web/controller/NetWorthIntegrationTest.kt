package com.pennypincher.pro.networth.infrastructure.web.controller

import com.jayway.jsonpath.JsonPath
import com.pennypincher.pro.networth.infrastructure.persistence.repository.NetWorthSnapshotJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.time.LocalDate
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class NetWorthIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var snapshots: NetWorthSnapshotJpaRepository

    @Test
    fun should_return_live_totals_of_own_active_items_when_requesting_current_net_worth() {
        val user = createUser()
        current(user).andExpect {
            status { isOk() }
            content {
                string(containsString(""""data":{"totalAssets":0.00,"totalLiabilities":0.00,"netWorth":0.00}"""))
            }
        }
        addBalanceSheet(user)
        createUserWithItems()

        current(user).andExpect {
            status { isOk() }
            content { string(containsString(""""totalAssets":411000000.25""")) }
            content { string(containsString(""""totalLiabilities":42500000.00""")) }
            content { string(containsString(""""netWorth":368500000.25""")) }
        }
        assertEquals(0, snapshots.findAll().count { it.userId == user.id })
    }

    @Test
    fun should_create_snapshot_for_the_month_then_recalculate_it_idempotently() {
        val user = createUser()
        addBalanceSheet(user)

        val created = saveSnapshot(user, "2026-10-21").andExpect {
            status { isCreated() }
            jsonPath("$.data.snapshotDate") { value("2026-10-01") }
            content { string(containsString(""""totalAssets":411000000.25,"totalLiabilities":42500000.00""")) }
            content { string(containsString(""""netWorth":368500000.25""")) }
        }
        val id = idOf(created)
        val createdAt: String = JsonPath.read(created.andReturn().response.contentAsString, "$.data.createdAt")
        postJson(user, ASSETS, """{"name":"Bono","value":1000000,"category":"CASH"}""")

        saveSnapshot(user, "2026-10-01").andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(id.toString()) }
            jsonPath("$.data.createdAt") { value(createdAt) }
            content { string(containsString(""""totalAssets":412000000.25""")) }
            content { string(containsString(""""netWorth":369500000.25""")) }
        }
        val stored = snapshots.findAll().single { it.userId == user.id }
        assertEquals(id, stored.id)
        assertEquals(LocalDate.parse("2026-10-01"), stored.snapshotDate)
        assertEquals("412000000.25", stored.totalAssets.toPlainString())
        assertEquals("369500000.25", stored.netWorth.toPlainString())
    }

    @Test
    fun should_list_own_snapshots_oldest_first_and_filter_by_inclusive_range() {
        val user = createUser()
        listOf("2026-10-05", "2024-01-31", "2025-06-01", "2021-10-01").forEach { saveSnapshot(user, it) }
        saveSnapshot(createUser(), "2025-06-01")

        snapshotList(user, "").andExpect {
            status { isOk() }
            jsonPath("$.data[*].snapshotDate") {
                value(contains("2021-10-01", "2024-01-01", "2025-06-01", "2026-10-01"))
            }
        }
        snapshotList(user, "?from=2024-01-15&to=2025-06-01").andExpect {
            jsonPath("$.data[*].snapshotDate") { value(contains("2024-01-01", "2025-06-01")) }
        }
        snapshotList(user, "?from=2025-01-01").andExpect {
            jsonPath("$.data[*].snapshotDate") { value(contains("2025-06-01", "2026-10-01")) }
        }
        snapshotList(user, "?to=2021-12-31").andExpect {
            jsonPath("$.data[*].snapshotDate") { value(contains("2021-10-01")) }
        }
    }

    @Test
    fun should_return_validation_error_when_dates_are_missing_invalid_or_reversed() {
        val user = createUser()
        mapOf(
            """{}""" to "snapshotDate",
            """{"snapshotDate":"2026-13-01"}""" to "snapshotDate",
        ).forEach { (body, field) ->
            postJson(user, SNAPSHOTS, body).andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                jsonPath("$.error.details[0].field") { value(field) }
            }
        }
        mapOf("?from=2026-02-01&to=2026-01-31" to "from", "?from=yesterday" to "from", "?to=2026-02-30" to "to")
            .forEach { (query, field) ->
                snapshotList(user, query).andExpect {
                    status { isBadRequest() }
                    jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                    jsonPath("$.error.details[0].field") { value(field) }
                }
            }
    }

    @Test
    fun should_return_unauthorized_when_token_is_missing() {
        listOf(
            mockMvc.get(CURRENT),
            mockMvc.get(SNAPSHOTS),
            mockMvc.post(SNAPSHOTS) { json("""{"snapshotDate":"2026-10-01"}""") },
        ).forEach { result ->
            result.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error.code") { value("UNAUTHORIZED") }
            }
        }
    }

    private fun addBalanceSheet(user: User) {
        postJson(user, ASSETS, """{"name":"Apto","value":350000000.25,"category":"REAL_ESTATE"}""")
        postJson(user, ASSETS, """{"name":"Carro","value":61000000,"category":"VEHICLE"}""")
        deleteAs(user, "$ASSETS/${idOf(postJson(user, ASSETS, """{"name":"X","value":5,"category":"CASH"}"""))}")
        postJson(user, LIABILITIES, """{"name":"Visa","currentBalance":2500000,"category":"CREDIT_CARD"}""")
        postJson(user, LIABILITIES, """{"name":"Banco","currentBalance":40000000,"category":"BANK_LOAN"}""")
    }

    private fun createUserWithItems() {
        val other = createUser()
        postJson(other, ASSETS, """{"name":"Ajeno","value":999,"category":"CASH"}""")
        postJson(other, LIABILITIES, """{"name":"Ajena","currentBalance":7,"category":"OTHER"}""")
    }

    private fun current(user: User): ResultActionsDsl = mockMvc.get(CURRENT) { bearer(user) }

    private fun saveSnapshot(user: User, date: String): ResultActionsDsl =
        postJson(user, SNAPSHOTS, """{"snapshotDate":"$date"}""")

    private fun snapshotList(user: User, query: String): ResultActionsDsl = mockMvc.get("$SNAPSHOTS$query") {
        bearer(user)
    }

    private companion object {
        const val ASSETS = "/api/v1/assets"
        const val LIABILITIES = "/api/v1/liabilities"
        const val CURRENT = "/api/v1/net-worth/current"
        const val SNAPSHOTS = "/api/v1/net-worth/snapshots"
    }
}
