package com.pennypincher.pro.shared.infrastructure.persistence.repository

import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import java.math.BigDecimal
import java.util.UUID
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param

/** Queries of the [CategorizedAmountEntity] tables on top of [OwnedRecordJpaRepository]; all scoped by user. */
@NoRepositoryBean
interface CategorizedAmountJpaRepository<E : CategorizedAmountEntity> : OwnedRecordJpaRepository<E> {
    fun findAllByUserIdAndCategoryAndActiveTrue(userId: UUID, category: String): List<E>

    @Query("select coalesce(sum(e.amount), 0) from #{#entityName} e where e.userId = :userId and e.active = true")
    fun sumActiveAmount(@Param("userId") userId: UUID): BigDecimal
}
