package com.pennypincher.pro.user.infrastructure.config

import com.pennypincher.pro.user.application.usecase.UserProfileService
import com.pennypincher.pro.user.domain.port.inbound.UserProfileUseCase
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Wires the framework-free user use cases. */
@Configuration
class UserUseCaseConfig {
    @Bean
    fun userProfileUseCase(userRepository: UserRepository): UserProfileUseCase = UserProfileService(userRepository)
}
