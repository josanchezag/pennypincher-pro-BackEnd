package com.pennypincher.pro.support

import com.pennypincher.pro.auth.domain.model.VerifiedIdentity
import com.pennypincher.pro.user.domain.model.IdentityProfile
import com.pennypincher.pro.user.domain.model.User
import java.time.Instant
import java.util.UUID

/** Shared sample data for auth and user tests. */
object AuthFixtures {
    val NOW: Instant = Instant.parse("2026-10-21T14:30:00Z")
    val PROFILE = IdentityProfile("ana@example.com", "Ana Pérez", "https://lh3.googleusercontent.com/a/ana")
    val IDENTITY = VerifiedIdentity("google-sub-123", PROFILE)

    fun user(id: UUID = UUID.randomUUID(), profile: IdentityProfile = PROFILE): User = User(
        id = id,
        googleSub = IDENTITY.subject,
        email = profile.email,
        name = profile.name,
        pictureUrl = profile.pictureUrl,
        createdAt = NOW,
    )
}
