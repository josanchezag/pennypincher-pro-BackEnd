package com.pennypincher.pro.expenses.infrastructure.persistence.adapter

import com.pennypincher.pro.expenses.domain.model.BalanceAccount
import com.pennypincher.pro.expenses.domain.model.BalanceTotals
import com.pennypincher.pro.expenses.domain.port.outbound.BalanceAccountRepository
import com.pennypincher.pro.expenses.infrastructure.persistence.entity.BalanceAccountEntity
import com.pennypincher.pro.expenses.infrastructure.persistence.repository.BalanceAccountJpaRepository
import com.pennypincher.pro.shared.infrastructure.persistence.adapter.OwnedRecordRepositoryAdapter
import java.util.UUID

/** Adapts a [BalanceAccountJpaRepository] to a domain [BalanceAccountRepository] port. */
abstract class BalanceAccountRepositoryAdapter<T : BalanceAccount, E : BalanceAccountEntity>(
    private val jpaRepository: BalanceAccountJpaRepository<E>,
) : OwnedRecordRepositoryAdapter<T, E, Nothing>(jpaRepository),
    BalanceAccountRepository<T> {
    override fun findAllActive(userId: UUID, category: Nothing?): List<T> = allActive(userId)

    override fun summarizeActive(userId: UUID): BalanceTotals {
        val row = jpaRepository.summarizeActive(userId)
        return BalanceTotals(row.totalMonthlyAmount, row.totalBalance, row.count)
    }
}
