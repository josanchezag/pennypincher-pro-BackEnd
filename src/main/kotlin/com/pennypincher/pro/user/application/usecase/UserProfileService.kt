package com.pennypincher.pro.user.application.usecase

import com.pennypincher.pro.user.domain.exception.UserNotFoundException
import com.pennypincher.pro.user.domain.model.ProfileUpdate
import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.domain.port.inbound.UserProfileUseCase
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import java.util.UUID

/** Profile use cases. E-mail and picture are managed by Google and cannot be changed here. */
class UserProfileService(private val userRepository: UserRepository) : UserProfileUseCase {
    override fun getProfile(userId: UUID): User = userRepository.findById(userId) ?: throw UserNotFoundException()

    override fun updateProfile(userId: UUID, update: ProfileUpdate): User {
        val current = getProfile(userId)
        val updated = update.name?.let(current::rename) ?: current
        return if (updated == current) current else userRepository.save(updated)
    }
}
