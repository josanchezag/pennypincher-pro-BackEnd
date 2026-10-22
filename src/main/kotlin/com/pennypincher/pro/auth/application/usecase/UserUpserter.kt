package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.model.VerifiedIdentity
import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Creates the user on first sign-in, or refreshes the Google-managed profile fields afterwards. */
class UserUpserter(private val userRepository: UserRepository, private val clock: Clock) {
    fun upsert(identity: VerifiedIdentity): User {
        val existing = userRepository.findByGoogleSub(identity.subject)
        val user =
            existing?.withIdentityProfile(identity.profile)
                ?: User.register(identity.subject, identity.profile, Instant.now(clock).truncatedTo(ChronoUnit.MICROS))
        return if (user == existing) existing else userRepository.save(user)
    }
}
