package com.pennypincher.pro.shared.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Columns of the "name, amount, category" tables on top of [OwnedRecordEntity]. Subclasses rename the
 * `amount` column with `@AttributeOverride`. The category is kept as its enum name.
 */
@MappedSuperclass
abstract class CategorizedAmountEntity(
    id: UUID,
    userId: UUID,
    name: String,
    @Column(name = "amount", nullable = false)
    var amount: BigDecimal,
    @Column(name = "category", nullable = false)
    var category: String,
    active: Boolean,
    createdAt: Instant,
    updatedAt: Instant,
) : OwnedRecordEntity(id, userId, name, active, createdAt, updatedAt)
