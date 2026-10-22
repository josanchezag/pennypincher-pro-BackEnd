package com.pennypincher.pro.user.infrastructure.persistence.adapter

import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import com.pennypincher.pro.user.infrastructure.persistence.repository.UserJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Adapts Spring Data JPA to the domain [UserRepository] port. */
@Component
class UserRepositoryAdapter(private val jpaRepository: UserJpaRepository) : UserRepository {
    @Transactional(readOnly = true)
    override fun findById(id: UUID): User? = jpaRepository.findById(id).map(UserEntityMapper::toDomain).orElse(null)

    @Transactional(readOnly = true)
    override fun findByGoogleSub(googleSub: String): User? =
        jpaRepository.findByGoogleSub(googleSub)?.let(UserEntityMapper::toDomain)

    @Transactional
    override fun save(user: User): User = UserEntityMapper.toDomain(jpaRepository.save(UserEntityMapper.toEntity(user)))
}
