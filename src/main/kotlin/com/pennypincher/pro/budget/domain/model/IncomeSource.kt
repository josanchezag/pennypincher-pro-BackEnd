package com.pennypincher.pro.budget.domain.model

import com.pennypincher.pro.budget.domain.model.enums.IncomeCategory
import com.pennypincher.pro.shared.domain.model.OwnedResource
import com.pennypincher.pro.shared.domain.model.RecordName
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** A recurring income stream with its monthly amount in COP; [currency] is only a reference code. */
data class IncomeSource(
    override val id: UUID,
    override val userId: UUID,
    val name: String,
    val category: IncomeCategory,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) : OwnedResource {
    companion object {
        /** Longest income source name the database accepts. */
        const val MAX_NAME_LENGTH = RecordName.MAX_LENGTH

        /** Currency used when the user does not give one. */
        const val DEFAULT_CURRENCY = "COP"

        /** ISO 4217 shape: three upper-case letters. */
        val CURRENCY_FORMAT = Regex("^[A-Z]{3}$")

        /** List order: category (declaration order), then name ignoring case. */
        val DISPLAY_ORDER: Comparator<IncomeSource> =
            compareBy<IncomeSource> { it.category }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}

/** Data a user supplies to create or update an income source; a null [currency] means the default one. */
data class IncomeSourceCommand(
    val name: String,
    val category: IncomeCategory,
    val amount: BigDecimal,
    val currency: String? = null,
)
