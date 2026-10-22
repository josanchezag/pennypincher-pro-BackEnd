package com.pennypincher.pro.networth.infrastructure.persistence.adapter

import com.pennypincher.pro.networth.domain.model.NetWorthSnapshot
import com.pennypincher.pro.networth.domain.model.NetWorthSummary
import com.pennypincher.pro.networth.domain.model.SnapshotSaveResult
import com.pennypincher.pro.networth.domain.port.outbound.NetWorthSnapshotRepository
import com.pennypincher.pro.networth.infrastructure.persistence.entity.NetWorthSnapshotEntity
import com.pennypincher.pro.networth.infrastructure.persistence.repository.NetWorthSnapshotJpaRepository
import java.sql.ResultSet
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Adapts the `net_worth_snapshots` table to the domain [NetWorthSnapshotRepository] port. Reads go through
 * Spring Data JPA; the upsert is one PostgreSQL `INSERT ... ON CONFLICT DO UPDATE` statement, so concurrent
 * saves of the same month never collide. `xmax = 0` in `RETURNING` is true only for a freshly inserted row.
 */
@Component
class NetWorthSnapshotRepositoryAdapter(
    private val jpaRepository: NetWorthSnapshotJpaRepository,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : NetWorthSnapshotRepository {
    @Transactional(readOnly = true)
    override fun findAll(userId: UUID, from: LocalDate?, to: LocalDate?): List<NetWorthSnapshot> =
        jpaRepository.findInRange(userId, from, to).map(::toDomain)

    @Transactional
    override fun upsert(snapshot: NetWorthSnapshot): SnapshotSaveResult =
        checkNotNull(jdbcTemplate.queryForObject(UPSERT_SQL, parametersOf(snapshot)) { row, _ -> toResult(row) })

    private fun parametersOf(snapshot: NetWorthSnapshot) = MapSqlParameterSource()
        .addValue("id", snapshot.id)
        .addValue("userId", snapshot.userId)
        .addValue("snapshotDate", snapshot.snapshotDate)
        .addValue("totalAssets", snapshot.totalAssets)
        .addValue("totalLiabilities", snapshot.totalLiabilities)
        .addValue("netWorth", snapshot.netWorth)
        .addValue("now", OffsetDateTime.ofInstant(snapshot.updatedAt, ZoneOffset.UTC))

    private fun toResult(row: ResultSet): SnapshotSaveResult {
        val snapshot = NetWorthSnapshot(
            id = row.getObject("id", UUID::class.java),
            userId = row.getObject("user_id", UUID::class.java),
            snapshotDate = row.getObject("snapshot_date", LocalDate::class.java),
            summary = NetWorthSummary(row.getBigDecimal("total_assets"), row.getBigDecimal("total_liabilities")),
            createdAt = row.getObject("created_at", OffsetDateTime::class.java).toInstant(),
            updatedAt = row.getObject("updated_at", OffsetDateTime::class.java).toInstant(),
        )
        return SnapshotSaveResult(snapshot, created = row.getBoolean("inserted"))
    }

    private fun toDomain(entity: NetWorthSnapshotEntity) = NetWorthSnapshot(
        id = entity.id,
        userId = entity.userId,
        snapshotDate = entity.snapshotDate,
        summary = NetWorthSummary(entity.totalAssets, entity.totalLiabilities),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    private companion object {
        val UPSERT_SQL =
            """
            INSERT INTO net_worth_snapshots
                (id, user_id, snapshot_date, total_assets, total_liabilities, net_worth, created_at, updated_at)
            VALUES (:id, :userId, :snapshotDate, :totalAssets, :totalLiabilities, :netWorth, :now, :now)
            ON CONFLICT (user_id, snapshot_date) DO UPDATE SET
                total_assets = EXCLUDED.total_assets,
                total_liabilities = EXCLUDED.total_liabilities,
                net_worth = EXCLUDED.net_worth,
                updated_at = EXCLUDED.updated_at
            RETURNING id, user_id, snapshot_date, total_assets, total_liabilities, created_at, updated_at,
                (xmax = 0) AS inserted
            """.trimIndent()
    }
}
