package com.pennypincher.pro.user.domain.model

import java.time.Instant
import java.util.UUID

/** A person who signed in with Google. `googleSub` is the stable Google account identifier. */
data class User(
    val id: UUID,
    val googleSub: String,
    val email: String,
    val name: String,
    val pictureUrl: String?,
    val createdAt: Instant,
) {
    /** Applies the latest profile data reported by Google on sign-in. */
    fun withIdentityProfile(profile: IdentityProfile): User =
        copy(email = profile.email, name = profile.name, pictureUrl = profile.pictureUrl)

    companion object {
        /** Longest display name the application stores. */
        const val MAX_NAME_LENGTH = 100

        fun register(googleSub: String, profile: IdentityProfile, now: Instant): User = User(
            id = UUID.randomUUID(),
            googleSub = googleSub,
            email = profile.email,
            name = profile.name,
            pictureUrl = profile.pictureUrl,
            createdAt = now,
        )
    }
}

/** Profile fields owned by the identity provider. */
data class IdentityProfile(val email: String, val name: String, val pictureUrl: String?)
