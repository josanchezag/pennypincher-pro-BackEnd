package com.pennypincher.pro.expenses.infrastructure.persistence.adapter

import com.pennypincher.pro.expenses.domain.model.Loan
import com.pennypincher.pro.expenses.domain.port.outbound.LoanRepository
import com.pennypincher.pro.expenses.infrastructure.persistence.entity.LoanEntity
import com.pennypincher.pro.expenses.infrastructure.persistence.repository.LoanJpaRepository
import org.springframework.stereotype.Component

/** Adapts Spring Data JPA to the domain [LoanRepository] port. Only active loans are ever read. */
@Component
class LoanRepositoryAdapter(jpaRepository: LoanJpaRepository) :
    BalanceAccountRepositoryAdapter<Loan, LoanEntity>(jpaRepository),
    LoanRepository {
    override fun toDomain(entity: LoanEntity) = Loan(
        id = entity.id,
        userId = entity.userId,
        name = entity.name,
        monthlyPayment = entity.monthlyAmount,
        totalBalance = entity.balance,
        originalAmount = entity.originalAmount,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(item: Loan) = LoanEntity(
        id = item.id,
        userId = item.userId,
        name = item.name,
        monthlyPayment = item.monthlyPayment,
        totalBalance = item.totalBalance,
        originalAmount = item.originalAmount,
        active = true,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
