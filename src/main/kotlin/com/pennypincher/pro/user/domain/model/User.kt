package com.pennypincher.pro.user.domain.model

import com.pennypincher.pro.shared.domain.exception.ValidationException
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

    /** Renames the user; the name is trimmed and must keep between 1 and [MAX_NAME_LENGTH] characters. */
    fun rename(newName: String): User {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH) {
            throw ValidationException(NAME_FIELD, "must be between 1 and $MAX_NAME_LENGTH characters")
        }
        return copy(name = trimmed)
    }

    companion object {
        /** Longest display name the application stores. */
        const val MAX_NAME_LENGTH = 100
        private const val NAME_FIELD = "name"

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
