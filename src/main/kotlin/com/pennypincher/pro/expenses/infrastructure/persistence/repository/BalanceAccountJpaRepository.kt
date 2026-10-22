package com.pennypincher.pro.expenses.infrastructure.persistence.repository

import com.pennypincher.pro.expenses.infrastructure.persistence.entity.BalanceAccountEntity
import com.pennypincher.pro.shared.infrastructure.persistence.repository.OwnedRecordJpaRepository
import java.math.BigDecimal
import java.util.UUID
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param

/** Queries of the [BalanceAccountEntity] tables on top of [OwnedRecordJpaRepository]; all scoped by user. */
@NoRepositoryBean
interface BalanceAccountJpaRepository<E : BalanceAccountEntity> : OwnedRecordJpaRepository<E> {
    @Query(
        "select coalesce(sum(e.monthlyAmount), 0) as totalMonthlyAmount, " +
            "coalesce(sum(e.balance), 0) as totalBalance, count(e) as count " +
            "from #{#entityName} e where e.userId = :userId and e.active = true",
    )
    fun summarizeActive(@Param("userId") userId: UUID): BalanceTotalsRow
}

/** One-row result of [BalanceAccountJpaRepository.summarizeActive]. */
interface BalanceTotalsRow {
    val totalMonthlyAmount: BigDecimal
    val totalBalance: BigDecimal
    val count: Long
}
