package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.support.AuthFixtures
import com.pennypincher.pro.user.domain.model.IdentityProfile
import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Clock
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class UserUpserterTest {
    private val userRepository = mockk<UserRepository>()
    private val upserter = UserUpserter(userRepository, Clock.fixed(AuthFixtures.NOW, ZoneOffset.UTC))

    @Test
    fun should_create_user_when_google_sub_is_new() {
        val saved = slot<User>()
        every { userRepository.findByGoogleSub("google-sub-123") } returns null
        every { userRepository.save(capture(saved)) } answers { saved.captured }

        val user = upserter.upsert(AuthFixtures.IDENTITY)

        assertEquals("google-sub-123", user.googleSub)
        assertEquals("ana@example.com", user.email)
        assertEquals("Ana Pérez", user.name)
        assertEquals("https://lh3.googleusercontent.com/a/ana", user.pictureUrl)
        assertEquals(AuthFixtures.NOW, user.createdAt)
    }

    @Test
    fun should_update_profile_when_user_already_exists() {
        val existing = AuthFixtures.user(profile = IdentityProfile("old@example.com", "Old Name", null))
        every { userRepository.findByGoogleSub("google-sub-123") } returns existing
        every { userRepository.save(any()) } answers { firstArg() }

        val user = upserter.upsert(AuthFixtures.IDENTITY)

        assertEquals(existing.id, user.id)
        assertEquals(existing.createdAt, user.createdAt)
        assertEquals(AuthFixtures.PROFILE.email, user.email)
        assertEquals(AuthFixtures.PROFILE.name, user.name)
        assertEquals(AuthFixtures.PROFILE.pictureUrl, user.pictureUrl)
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun should_skip_saving_when_profile_is_unchanged() {
        val existing = AuthFixtures.user()
        every { userRepository.findByGoogleSub("google-sub-123") } returns existing

        assertSame(existing, upserter.upsert(AuthFixtures.IDENTITY))
        verify(exactly = 0) { userRepository.save(any()) }
    }
}
