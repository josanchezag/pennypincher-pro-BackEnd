package com.pennypincher.pro.expenses.infrastructure.persistence.adapter

import com.pennypincher.pro.expenses.domain.model.SavingsAccount
import com.pennypincher.pro.expenses.domain.port.outbound.SavingsAccountRepository
import com.pennypincher.pro.expenses.infrastructure.persistence.entity.SavingsAccountEntity
import com.pennypincher.pro.expenses.infrastructure.persistence.repository.SavingsAccountJpaRepository
import org.springframework.stereotype.Component

/** Adapts Spring Data JPA to the domain [SavingsAccountRepository] port. Only active accounts are ever read. */
@Component
class SavingsAccountRepositoryAdapter(jpaRepository: SavingsAccountJpaRepository) :
    BalanceAccountRepositoryAdapter<SavingsAccount, SavingsAccountEntity>(jpaRepository),
    SavingsAccountRepository {
    override fun toDomain(entity: SavingsAccountEntity) = SavingsAccount(
        id = entity.id,
        userId = entity.userId,
        name = entity.name,
        monthlyContribution = entity.monthlyAmount,
        currentBalance = entity.balance,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(item: SavingsAccount) = SavingsAccountEntity(
        id = item.id,
        userId = item.userId,
        name = item.name,
        monthlyContribution = item.monthlyContribution,
        currentBalance = item.currentBalance,
        active = true,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
