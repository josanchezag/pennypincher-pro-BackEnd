package com.pennypincher.pro.shared.infrastructure.persistence.repository

import com.pennypincher.pro.shared.infrastructure.persistence.entity.CategorizedAmountEntity
import java.math.BigDecimal
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param

/** Spring Data queries shared by every [CategorizedAmountEntity] table; all of them are scoped by user. */
@NoRepositoryBean
interface CategorizedAmountJpaRepository<E : CategorizedAmountEntity> : JpaRepository<E, UUID> {
    fun findAllByUserIdAndActiveTrue(userId: UUID): List<E>

    fun findAllByUserIdAndCategoryAndActiveTrue(userId: UUID, category: String): List<E>

    fun findByIdAndUserIdAndActiveTrue(id: UUID, userId: UUID): E?

    @Modifying
    @Query("update #{#entityName} e set e.active = false where e.id = :id and e.userId = :userId and e.active = true")
    fun deactivate(@Param("id") id: UUID, @Param("userId") userId: UUID): Int

    @Query("select coalesce(sum(e.amount), 0) from #{#entityName} e where e.userId = :userId and e.active = true")
    fun sumActiveAmount(@Param("userId") userId: UUID): BigDecimal
}
