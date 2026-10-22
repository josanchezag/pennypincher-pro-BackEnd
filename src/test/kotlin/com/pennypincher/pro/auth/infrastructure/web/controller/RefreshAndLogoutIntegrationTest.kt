package com.pennypincher.pro.auth.infrastructure.web.controller

import com.pennypincher.pro.auth.application.usecase.RefreshTokenCodec
import com.pennypincher.pro.auth.application.usecase.TokenPairIssuer
import com.pennypincher.pro.auth.domain.model.RefreshToken
import com.pennypincher.pro.auth.domain.port.outbound.RefreshTokenRepository
import com.pennypincher.pro.auth.infrastructure.persistence.repository.RefreshTokenJpaRepository
import com.pennypincher.pro.support.AuthFixtures
import com.pennypincher.pro.support.IntegrationTest
import com.pennypincher.pro.user.domain.model.IdentityProfile
import com.pennypincher.pro.user.domain.model.User
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import jakarta.servlet.http.Cookie
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post

class RefreshAndLogoutIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var tokenPairIssuer: TokenPairIssuer

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var refreshTokens: RefreshTokenJpaRepository

    private val codec = RefreshTokenCodec()

    @Test
    fun should_rotate_tokens_when_refresh_cookie_is_valid() {
        val (user, oldToken) = signedInUser()

        val response = refresh(oldToken).andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { isNotEmpty() }
            jsonPath("$.data.tokenType") { value("Bearer") }
            jsonPath("$.data.user.id") { value(user.id.toString()) }
        }.andReturn().response

        val newToken = cookieValue(response.getHeader(HttpHeaders.SET_COOKIE).orEmpty())
        assertNotEquals(oldToken, newToken)
        assertTrue(refreshTokens.findByTokenHash(codec.hash(oldToken))!!.revoked)
        assertTrue(refreshTokens.findByTokenHash(codec.hash(oldToken))!!.revokedAt != null)
        assertEquals(false, refreshTokens.findByTokenHash(codec.hash(newToken))!!.revoked)
    }

    @Test
    fun should_revoke_every_session_when_a_rotated_token_is_reused_after_the_grace_period() {
        val (_, oldToken) = signedInUser()
        val newToken = rotate(oldToken)
        val rotated = refreshTokens.findByTokenHash(codec.hash(oldToken))!!
        rotated.revokedAt = Instant.now().minus(Duration.ofMinutes(1))
        refreshTokens.save(rotated)

        refresh(oldToken).andExpectRejectedAndCleared()

        assertNull(refreshTokens.findByTokenHash(codec.hash(newToken)))
        refresh(newToken).andExpectRejectedAndCleared()
    }

    @Test
    fun should_keep_session_and_cookie_when_just_rotated_token_is_presented_again() {
        val (_, oldToken) = signedInUser()
        val newToken = rotate(oldToken)

        refresh(oldToken).andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("REFRESH_SUPERSEDED") }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }

        assertEquals(false, refreshTokens.findByTokenHash(codec.hash(newToken))!!.revoked)
        refresh(newToken).andExpect { status { isOk() } }
    }

    @Test
    fun should_keep_winner_session_when_refreshes_run_in_parallel() {
        val (_, token) = signedInUser()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(PARALLEL_REFRESHES)
        val responses = try {
            (1..PARALLEL_REFRESHES).map {
                pool.submit<MockHttpServletResponse> {
                    start.await()
                    refresh(token).andReturn().response
                }
            }
                .also { start.countDown() }
                .map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }

        val (winners, losers) = responses.partition { it.status == HttpStatus.OK.value() }
        assertEquals(1, winners.size)
        assertTrue(
            losers.all {
                it.status == HttpStatus.UNAUTHORIZED.value() &&
                    it.getHeader(HttpHeaders.SET_COOKIE) == null &&
                    "\"REFRESH_SUPERSEDED\"" in it.contentAsString
            },
        )
        val winnerToken = cookieValue(winners.single().getHeader(HttpHeaders.SET_COOKIE).orEmpty())
        assertEquals(false, refreshTokens.findByTokenHash(codec.hash(winnerToken))!!.revoked)
        refresh(winnerToken).andExpect { status { isOk() } }
    }

    @Test
    fun should_reject_refresh_when_cookie_is_missing_or_unknown() {
        mockMvc.post(REFRESH_PATH) { header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN) }.andExpectRejectedAndCleared()
        refresh("never-issued").andExpectRejectedAndCleared()
    }

    @Test
    fun should_reject_refresh_when_token_is_expired() {
        val user = createUser()
        val raw = codec.generate()
        val createdAt = Instant.now().minus(Duration.ofDays(31))
        refreshTokenRepository.save(
            RefreshToken.issue(user.id, codec.hash(raw), createdAt, createdAt.plus(Duration.ofDays(30))),
        )

        refresh(raw).andExpectRejectedAndCleared()
    }

    @Test
    fun should_delete_token_and_clear_cookie_when_logging_out() {
        val (user, token) = signedInUser()
        val otherDevice = tokenPairIssuer.issueFor(user).refreshToken

        logout(token).andExpect {
            status { isNoContent() }
            header { string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")) }
        }

        assertNull(refreshTokens.findByTokenHash(codec.hash(token)))
        refresh(token).andExpectRejectedAndCleared()
        refresh(otherDevice).andExpect { status { isOk() } }
    }

    @Test
    fun should_succeed_when_logging_out_without_cookie_or_twice() {
        mockMvc.delete(LOGOUT_PATH) {
            header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
        }.andExpect { status { isNoContent() } }
        val (_, token) = signedInUser()
        repeat(2) { logout(token).andExpect { status { isNoContent() } } }
    }

    @Test
    fun should_reject_cookie_endpoints_when_origin_is_missing_or_not_allowed() {
        val (_, token) = signedInUser()
        listOf(null, "https://evil.example.com").forEach { origin ->
            mockMvc.post(REFRESH_PATH) {
                cookie(Cookie(COOKIE, token))
                origin?.let { header(HttpHeaders.ORIGIN, it) }
            }.andExpectForbiddenOrigin()
            mockMvc.delete(LOGOUT_PATH) {
                cookie(Cookie(COOKIE, token))
                origin?.let { header(HttpHeaders.ORIGIN, it) }
            }.andExpectForbiddenOrigin()
        }
        assertEquals(false, refreshTokens.findByTokenHash(codec.hash(token))!!.revoked)
    }

    private fun rotate(token: String): String =
        cookieValue(refresh(token).andReturn().response.getHeader(HttpHeaders.SET_COOKIE).orEmpty())

    private fun signedInUser(): Pair<User, String> {
        val user = createUser()
        return user to tokenPairIssuer.issueFor(user).refreshToken
    }

    private fun createUser(): User {
        val suffix = UUID.randomUUID()
        val profile = IdentityProfile("user-$suffix@example.com", "Refresh User", null)
        return userRepository.save(User.register("sub-$suffix", profile, AuthFixtures.NOW))
    }

    private fun refresh(token: String): ResultActionsDsl = mockMvc.post(REFRESH_PATH) {
        cookie(Cookie(COOKIE, token))
        header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
    }

    private fun logout(token: String): ResultActionsDsl = mockMvc.delete(LOGOUT_PATH) {
        cookie(Cookie(COOKIE, token))
        header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
    }

    private fun ResultActionsDsl.andExpectRejectedAndCleared() = andExpect {
        status { isUnauthorized() }
        jsonPath("$.error.code") { value("UNAUTHORIZED") }
        header { string(HttpHeaders.SET_COOKIE, containsString("$COOKIE=;")) }
        header { string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")) }
    }

    private fun ResultActionsDsl.andExpectForbiddenOrigin() = andExpect {
        status { isForbidden() }
        jsonPath("$.error.code") { value("FORBIDDEN_ORIGIN") }
    }

    private fun cookieValue(header: String) = header.substringAfter("$COOKIE=").substringBefore(";")

    private companion object {
        const val COOKIE = "ppp_refresh"
        const val REFRESH_PATH = "/api/v1/auth/refresh"
        const val LOGOUT_PATH = "/api/v1/auth/logout"
        const val ALLOWED_ORIGIN = "http://localhost:3000"
        const val PARALLEL_REFRESHES = 5
        const val TIMEOUT_SECONDS = 30L
    }
}
