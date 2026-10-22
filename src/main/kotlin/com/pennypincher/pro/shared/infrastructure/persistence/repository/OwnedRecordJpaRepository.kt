package com.pennypincher.pro.shared.infrastructure.persistence.repository

import com.pennypincher.pro.shared.infrastructure.persistence.entity.OwnedRecordEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param

/** Spring Data queries shared by every [OwnedRecordEntity] table; all of them are scoped by user. */
@NoRepositoryBean
interface OwnedRecordJpaRepository<E : OwnedRecordEntity> : JpaRepository<E, UUID> {
    fun findAllByUserIdAndActiveTrue(userId: UUID): List<E>

    fun findByIdAndUserIdAndActiveTrue(id: UUID, userId: UUID): E?

    @Modifying
    @Query("update #{#entityName} e set e.active = false where e.id = :id and e.userId = :userId and e.active = true")
    fun deactivate(@Param("id") id: UUID, @Param("userId") userId: UUID): Int
}
