package com.pennypincher.pro.networth.infrastructure.persistence.repository

import com.pennypincher.pro.networth.infrastructure.persistence.entity.NetWorthSnapshotEntity
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NetWorthSnapshotJpaRepository : JpaRepository<NetWorthSnapshotEntity, UUID> {
    @Query(
        "select s from NetWorthSnapshotEntity s where s.userId = :userId " +
            "and (cast(:from as localdate) is null or s.snapshotDate >= :from) " +
            "and (cast(:to as localdate) is null or s.snapshotDate <= :to) order by s.snapshotDate",
    )
    fun findInRange(
        @Param("userId") userId: UUID,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?,
    ): List<NetWorthSnapshotEntity>
}
