package com.pennypincher.pro.expenses.infrastructure.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Public representation of a loan; `originalAmount` is always present, null when it is not tracked. */
data class LoanResponse(
    val id: UUID,
    val name: String,
    val monthlyPayment: BigDecimal,
    val totalBalance: BigDecimal,
    @field:JsonInclude(JsonInclude.Include.ALWAYS)
    val originalAmount: BigDecimal?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
