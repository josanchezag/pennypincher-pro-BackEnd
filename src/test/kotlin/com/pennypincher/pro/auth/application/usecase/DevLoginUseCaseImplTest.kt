package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.model.AccessToken
import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.support.AuthFixtures
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class DevLoginUseCaseImplTest {
    private val userUpserter = mockk<UserUpserter>()
    private val tokenPairIssuer = mockk<TokenPairIssuer>()
    private val useCase = DevLoginUseCaseImpl(userUpserter, tokenPairIssuer)

    @Test
    fun should_issue_tokens_for_fixed_test_user_when_dev_login_is_used() {
        val user = AuthFixtures.user()
        val expected = AuthResult(AccessToken("access", Duration.ofMinutes(15)), "refresh", user)
        every { userUpserter.upsert(DevLoginUseCaseImpl.DEV_IDENTITY) } returns user
        every { tokenPairIssuer.issueFor(user) } returns expected

        assertSame(expected, useCase.authenticateDevUser())
        assertEquals("dev-local-user", DevLoginUseCaseImpl.DEV_IDENTITY.subject)
        assertEquals("dev@pennypincher.local", DevLoginUseCaseImpl.DEV_IDENTITY.profile.email)
        assertEquals("Dev User", DevLoginUseCaseImpl.DEV_IDENTITY.profile.name)
    }
}
