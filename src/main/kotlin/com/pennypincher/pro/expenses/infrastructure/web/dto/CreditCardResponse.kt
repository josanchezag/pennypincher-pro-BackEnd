package com.pennypincher.pro.expenses.infrastructure.web.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Public representation of a credit card. */
data class CreditCardResponse(
    val id: UUID,
    val name: String,
    val monthlyPayment: BigDecimal,
    val totalBalance: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
)
