package com.pennypincher.pro.user.application.usecase

import com.pennypincher.pro.shared.domain.exception.ValidationException
import com.pennypincher.pro.support.AuthFixtures
import com.pennypincher.pro.user.domain.exception.UserNotFoundException
import com.pennypincher.pro.user.domain.model.ProfileUpdate
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserProfileServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val service = UserProfileService(userRepository)
    private val user = AuthFixtures.user()

    @Test
    fun should_return_profile_when_user_exists() {
        every { userRepository.findById(user.id) } returns user

        assertSame(user, service.getProfile(user.id))
    }

    @Test
    fun should_throw_not_found_when_user_does_not_exist() {
        every { userRepository.findById(user.id) } returns null

        assertThrows<UserNotFoundException> { service.getProfile(user.id) }
        assertThrows<UserNotFoundException> { service.updateProfile(user.id, ProfileUpdate("New")) }
    }

    @Test
    fun should_save_trimmed_name_when_updating_profile() {
        every { userRepository.findById(user.id) } returns user
        every { userRepository.save(any()) } answers { firstArg() }

        val updated = service.updateProfile(user.id, ProfileUpdate("  Ana María  "))

        assertEquals("Ana María", updated.name)
        assertEquals(user.email, updated.email)
        verify(exactly = 1) { userRepository.save(updated) }
    }

    @Test
    fun should_not_save_when_name_is_absent_or_unchanged() {
        every { userRepository.findById(user.id) } returns user

        assertSame(user, service.updateProfile(user.id, ProfileUpdate(null)))
        assertSame(user, service.updateProfile(user.id, ProfileUpdate(" ${user.name} ")))
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun should_reject_name_when_blank_or_longer_than_100_characters() {
        every { userRepository.findById(user.id) } returns user

        listOf("", "   ", "x".repeat(101)).forEach { name ->
            val exception = assertThrows<ValidationException> { service.updateProfile(user.id, ProfileUpdate(name)) }
            assertEquals("name", exception.violations.single().field)
        }
    }

    @Test
    fun should_accept_name_when_it_has_exactly_100_characters() {
        every { userRepository.findById(user.id) } returns user
        every { userRepository.save(any()) } answers { firstArg() }

        assertEquals(100, service.updateProfile(user.id, ProfileUpdate("y".repeat(100))).name.length)
    }
}
