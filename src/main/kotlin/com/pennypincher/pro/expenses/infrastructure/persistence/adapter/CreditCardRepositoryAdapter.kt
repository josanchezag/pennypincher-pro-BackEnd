package com.pennypincher.pro.expenses.infrastructure.persistence.adapter

import com.pennypincher.pro.expenses.domain.model.CreditCard
import com.pennypincher.pro.expenses.domain.port.outbound.CreditCardRepository
import com.pennypincher.pro.expenses.infrastructure.persistence.entity.CreditCardEntity
import com.pennypincher.pro.expenses.infrastructure.persistence.repository.CreditCardJpaRepository
import org.springframework.stereotype.Component

/** Adapts Spring Data JPA to the domain [CreditCardRepository] port. Only active cards are ever read. */
@Component
class CreditCardRepositoryAdapter(jpaRepository: CreditCardJpaRepository) :
    BalanceAccountRepositoryAdapter<CreditCard, CreditCardEntity>(jpaRepository),
    CreditCardRepository {
    override fun toDomain(entity: CreditCardEntity) = CreditCard(
        id = entity.id,
        userId = entity.userId,
        name = entity.name,
        monthlyPayment = entity.monthlyAmount,
        totalBalance = entity.balance,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(item: CreditCard) = CreditCardEntity(
        id = item.id,
        userId = item.userId,
        name = item.name,
        monthlyPayment = item.monthlyPayment,
        totalBalance = item.totalBalance,
        active = true,
        createdAt = item.createdAt,
        updatedAt = item.updatedAt,
    )
}
