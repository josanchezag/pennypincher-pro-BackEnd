package com.pennypincher.pro.networth.domain.model

import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.shared.domain.model.OwnedResource
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Something the user owns, with its current estimated market value in COP. */
data class Asset(
    override val id: UUID,
    override val userId: UUID,
    val name: String,
    val value: BigDecimal,
    val category: AssetCategory,
    val createdAt: Instant,
    val updatedAt: Instant,
) : OwnedResource {
    companion object {
        /** Longest asset name the database accepts. */
        const val MAX_NAME_LENGTH = 150

        /** List order: category (declaration order), then name ignoring case. */
        val DISPLAY_ORDER: Comparator<Asset> =
            compareBy<Asset> { it.category }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}

/** Data a user supplies to create or update an asset. */
data class AssetCommand(val name: String, val value: BigDecimal, val category: AssetCategory)
