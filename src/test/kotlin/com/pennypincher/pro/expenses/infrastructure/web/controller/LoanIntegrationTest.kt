package com.pennypincher.pro.expenses.infrastructure.web.controller

import com.pennypincher.pro.expenses.infrastructure.persistence.repository.LoanJpaRepository
import com.pennypincher.pro.support.ApiIntegrationTest
import com.pennypincher.pro.user.domain.model.User
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.get

class LoanIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var loans: LoanJpaRepository

    @Test
    fun should_create_loan_with_location_and_original_amount_when_given() {
        val user = createUser()

        val result = postJson(
            user,
            LOANS,
            """{"name":" Prestamo Occidente 190028884 ","monthlyPayment":1258908,"totalBalance":88614841,"originalAmount":120000000}""",
        )
        result.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("Prestamo Occidente 190028884") }
            jsonPath("$.data.userId") { doesNotExist() }
            content {
                string(containsString("\"monthlyPayment\":1258908.00"))
                string(containsString("\"totalBalance\":88614841.00"))
                string(containsString("\"originalAmount\":120000000.00"))
            }
        }
        val id = idOf(result)
        assertEquals("http://localhost$LOANS/$id", result.andReturn().response.getHeader(HttpHeaders.LOCATION))
        assertEquals("120000000.00", loans.findById(id).orElseThrow().originalAmount?.toPlainString())
    }

    @Test
    fun should_return_null_original_amount_when_not_tracked_and_clear_it_when_updating_without_it() {
        val user = createUser()
        postJson(user, LOANS, """{"name":"Davivienda 2670","monthlyPayment":681500,"totalBalance":22727489}""")
            .andExpect { content { string(containsString("\"originalAmount\":null")) } }
        val id = createLoan(user, "Davivienda 7141", originalAmount = "30000000")

        putJson(user, "$LOANS/$id", """{"name":"Davivienda 7141","monthlyPayment":1339823,"totalBalance":59781329}""")
            .andExpect {
                status { isOk() }
                content { string(containsString("\"originalAmount\":null")) }
            }
        assertNull(loans.findById(id).orElseThrow().originalAmount)
    }

    @Test
    fun should_return_validation_error_when_original_amount_is_invalid() {
        val user = createUser()
        listOf("-1", "1.001", "\"abc\"").forEach { amount ->
            postJson(user, LOANS, """{"name":"Banco","monthlyPayment":1,"totalBalance":1,"originalAmount":$amount}""")
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                    jsonPath("$.error.details[0].field") { value("originalAmount") }
                }
        }
    }

    @Test
    fun should_sort_by_name_and_sum_only_own_active_loans_when_listing_and_summarizing() {
        val user = createUser()
        createLoan(user, "Prestamo Occidente 190028884", "1258908", "88614841")
        createLoan(user, "Prestamo Davivienda 7141", "1339823", "59781329")
        createLoan(user, "prestamo Davivienda 2670", "681500", "22727489")
        deleteAs(user, "$LOANS/${createLoan(user, "Viejo", "999", "999")}")
        createLoan(createUser(), "Ajeno", "5", "5")

        mockMvc.get(LOANS) { bearer(user) }.andExpect {
            jsonPath("$.data[*].name") {
                value(contains("prestamo Davivienda 2670", "Prestamo Davivienda 7141", "Prestamo Occidente 190028884"))
            }
        }
        mockMvc.get("$LOANS/summary") { bearer(user) }.andExpect {
            status { isOk() }
            content {
                string(
                    containsString(
                        """{"totalMonthlyPayments":3280231.00,"totalOutstandingBalance":171123659.00,"count":3}""",
                    ),
                )
            }
        }
    }

    private fun createLoan(
        user: User,
        name: String,
        payment: String = "100",
        balance: String = "1000",
        originalAmount: String = "null",
    ): UUID = idOf(
        postJson(
            user,
            LOANS,
            """{"name":"$name","monthlyPayment":$payment,"totalBalance":$balance,"originalAmount":$originalAmount}""",
        ),
    )

    private companion object {
        const val LOANS = "/api/v1/loans"
    }
}
