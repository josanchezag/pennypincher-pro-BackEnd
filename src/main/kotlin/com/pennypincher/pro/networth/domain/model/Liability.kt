package com.pennypincher.pro.networth.domain.model

import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import com.pennypincher.pro.shared.domain.model.OwnedResource
import com.pennypincher.pro.shared.domain.model.RecordName
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** A debt the user owes, with its outstanding balance in COP. */
data class Liability(
    override val id: UUID,
    override val userId: UUID,
    val name: String,
    val currentBalance: BigDecimal,
    val category: LiabilityCategory,
    val createdAt: Instant,
    val updatedAt: Instant,
) : OwnedResource {
    companion object {
        /** Longest liability name the database accepts. */
        const val MAX_NAME_LENGTH = RecordName.MAX_LENGTH

        /** List order: category (declaration order), then name ignoring case. */
        val DISPLAY_ORDER: Comparator<Liability> =
            compareBy<Liability> { it.category }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}

/** Data a user supplies to create or update a liability. */
data class LiabilityCommand(val name: String, val currentBalance: BigDecimal, val category: LiabilityCategory)
