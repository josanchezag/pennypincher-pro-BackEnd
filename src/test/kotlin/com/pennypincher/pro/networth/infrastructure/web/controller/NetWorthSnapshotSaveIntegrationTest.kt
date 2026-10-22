package com.pennypincher.pro.networth.infrastructure.web.controller

import com.pennypincher.pro.networth.infrastructure.persistence.repository.NetWorthSnapshotJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

/** Saving snapshots: concurrent saves of one month, months in the future and totals that overflow. */
class NetWorthSnapshotSaveIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var snapshots: NetWorthSnapshotJpaRepository

    @Test
    fun should_save_one_row_and_answer_every_request_when_the_same_month_is_saved_concurrently() {
        val user = createUser()
        postJson(user, ASSETS, """{"name":"Apto","value":350000000,"category":"REAL_ESTATE"}""")
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(PARALLEL_REQUESTS)
        val statuses = try {
            val calls = (1..PARALLEL_REQUESTS).map {
                Callable {
                    start.await()
                    postJson(user, SNAPSHOTS, """{"snapshotDate":"2026-10-0$it"}""").andReturn().response.status
                }
            }
            val futures = calls.map(executor::submit)
            start.countDown()
            futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(1, statuses.count { it == HttpStatus.CREATED.value() }) { "Statuses: $statuses" }
        assertEquals(PARALLEL_REQUESTS - 1, statuses.count { it == HttpStatus.OK.value() }) { "Statuses: $statuses" }
        val stored = snapshots.findAll().filter { it.userId == user.id }
        assertEquals(listOf(LocalDate.parse("2026-10-01")), stored.map { it.snapshotDate })
        assertEquals("350000000.00", stored.single().netWorth.toPlainString())
    }

    @Test
    fun should_accept_current_month_and_reject_months_after_it() {
        val user = createUser()
        val today = LocalDate.now(ZoneOffset.UTC)

        postJson(user, SNAPSHOTS, """{"snapshotDate":"$today"}""").andExpect { status { isCreated() } }
        postJson(user, SNAPSHOTS, """{"snapshotDate":"${today.plusMonths(1).withDayOfMonth(1)}"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_FAILED") }
            jsonPath("$.error.details[0].field") { value("snapshotDate") }
        }
        assertEquals(1, snapshots.findAll().count { it.userId == user.id })
    }

    @Test
    fun should_return_validation_error_instead_of_conflict_when_total_assets_overflow() {
        val user = createUser()
        repeat(2) { postJson(user, ASSETS, """{"name":"Max $it","value":$MAX_AMOUNT,"category":"CASH"}""") }

        postJson(user, SNAPSHOTS, """{"snapshotDate":"2026-10-01"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_FAILED") }
            jsonPath("$.error.details[0].field") { value("totalAssets") }
        }
        assertEquals(0, snapshots.findAll().count { it.userId == user.id })
    }

    private companion object {
        const val ASSETS = "/api/v1/assets"
        const val SNAPSHOTS = "/api/v1/net-worth/snapshots"
        const val PARALLEL_REQUESTS = 8
        const val TIMEOUT_SECONDS = 60L
        const val MAX_AMOUNT = "9999999999999999.99"
    }
}
