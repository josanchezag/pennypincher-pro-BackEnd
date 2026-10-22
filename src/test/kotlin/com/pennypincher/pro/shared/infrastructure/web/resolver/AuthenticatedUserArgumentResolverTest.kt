package com.pennypincher.pro.shared.infrastructure.web.resolver

import com.pennypincher.pro.shared.domain.model.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.context.request.NativeWebRequest

class AuthenticatedUserArgumentResolverTest {
    private val resolver = AuthenticatedUserArgumentResolver()

    @Suppress("UNUSED_PARAMETER")
    fun handler(@AuthenticatedUser principal: UserPrincipal, plain: UserPrincipal, @AuthenticatedUser other: String) =
        Unit

    private val method = javaClass.getMethod(
        "handler",
        UserPrincipal::class.java,
        UserPrincipal::class.java,
        String::class.java,
    )

    @Test
    fun should_support_only_annotated_user_principal_parameters() {
        assertTrue(resolver.supportsParameter(MethodParameter(method, 0)))
        assertFalse(resolver.supportsParameter(MethodParameter(method, 1)))
        assertFalse(resolver.supportsParameter(MethodParameter(method, 2)))
    }

    @Test
    fun should_return_principal_when_request_is_authenticated() {
        val principal = UserPrincipal(UUID.randomUUID(), "ana@example.com")
        val request = mockk<NativeWebRequest>()
        every { request.userPrincipal } returns
            UsernamePasswordAuthenticationToken.authenticated(principal, null, emptyList())

        assertEquals(principal, resolver.resolveArgument(MethodParameter(method, 0), null, request, null))
    }

    @Test
    fun should_fail_authentication_when_request_has_no_principal() {
        val request = mockk<NativeWebRequest>()
        every { request.userPrincipal } returns null

        assertThrows<AuthenticationCredentialsNotFoundException> {
            resolver.resolveArgument(MethodParameter(method, 0), null, request, null)
        }
    }
}
