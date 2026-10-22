package com.pennypincher.pro.user.infrastructure.web.controller

import com.pennypincher.pro.auth.domain.port.outbound.AccessTokenIssuer
import com.pennypincher.pro.support.AuthFixtures
import com.pennypincher.pro.support.IntegrationTest
import com.pennypincher.pro.user.domain.model.IdentityProfile
import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import com.pennypincher.pro.user.infrastructure.persistence.repository.UserJpaRepository
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch

class UserProfileIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var users: UserJpaRepository

    @Autowired
    private lateinit var accessTokenIssuer: AccessTokenIssuer

    @Test
    fun should_return_own_profile_when_authenticated() {
        val user = createUser()

        mockMvc.get(ME_PATH) { bearer(user) }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(user.id.toString()) }
            jsonPath("$.data.email") { value(user.email) }
            jsonPath("$.data.name") { value("Profile User") }
            jsonPath("$.data.pictureUrl") { value("https://example.com/p.png") }
            jsonPath("$.data.createdAt") { value("2026-10-21T14:30:00Z") }
            jsonPath("$.meta.requestId") { isNotEmpty() }
        }
    }

    @Test
    fun should_update_only_name_when_body_also_has_google_managed_fields() {
        val user = createUser()

        patch(user, """{"name":"  New Name  ","email":"hacker@example.com","pictureUrl":"https://evil"}""").andExpect {
            status { isOk() }
            jsonPath("$.data.name") { value("New Name") }
            jsonPath("$.data.email") { value(user.email) }
            jsonPath("$.data.pictureUrl") { value("https://example.com/p.png") }
        }

        val stored = users.findById(user.id).orElseThrow()
        assertEquals("New Name", stored.name)
        assertEquals(user.email, stored.email)
    }

    @Test
    fun should_return_validation_error_when_name_is_blank_or_too_long() {
        val user = createUser()
        listOf("   ", "x".repeat(101)).forEach { name ->
            patch(user, """{"name":"$name"}""").andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("VALIDATION_FAILED") }
                jsonPath("$.error.details[0].field") { value("name") }
            }
        }
    }

    @Test
    fun should_return_not_found_when_user_row_was_deleted() {
        val user = createUser()
        users.deleteById(user.id)

        mockMvc.get(ME_PATH) { bearer(user) }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("NOT_FOUND") }
        }
        patch(user, """{"name":"Ghost"}""").andExpect { status { isNotFound() } }
    }

    @Test
    fun should_return_unauthorized_when_token_is_missing() {
        mockMvc.patch(ME_PATH) {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"x"}"""
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("UNAUTHORIZED") }
        }
    }

    private fun patch(user: User, body: String): ResultActionsDsl = mockMvc.patch(ME_PATH) {
        bearer(user)
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun MockHttpServletRequestDsl.bearer(user: User) {
        header(HttpHeaders.AUTHORIZATION, "Bearer ${accessTokenIssuer.issue(user).value}")
    }

    private fun createUser(): User {
        val suffix = UUID.randomUUID()
        val profile = IdentityProfile("profile-$suffix@example.com", "Profile User", "https://example.com/p.png")
        return userRepository.save(User.register("profile-sub-$suffix", profile, AuthFixtures.NOW))
    }

    private companion object {
        const val ME_PATH = "/api/v1/users/me"
    }
}
