package com.pennypincher.pro.auth.infrastructure.web

import com.pennypincher.pro.auth.infrastructure.config.AuthCookieProperties
import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtProperties
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RefreshCookieFactoryTest {
    private val jwtProperties = JwtProperties("pennypincher-pro", Duration.ofMinutes(15), Duration.ofDays(30))

    @Test
    fun should_build_http_only_secure_strict_cookie_when_defaults_apply() {
        val cookie = RefreshCookieFactory(AuthCookieProperties(), jwtProperties).create("token-value")

        assertEquals("ppp_refresh", cookie.name)
        assertEquals("token-value", cookie.value)
        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.isSecure)
        assertEquals("Strict", cookie.sameSite)
        assertEquals("/api/v1/auth", cookie.path)
        assertEquals(Duration.ofDays(30), cookie.maxAge)
    }

    @Test
    fun should_expire_cookie_immediately_when_clearing() {
        val cookie = RefreshCookieFactory(
            AuthCookieProperties(false, AuthCookieProperties.SameSite.LAX),
            jwtProperties,
        ).clear()

        assertEquals("", cookie.value)
        assertEquals(Duration.ZERO, cookie.maxAge)
        assertFalse(cookie.isSecure)
        assertEquals("Lax", cookie.sameSite)
    }

    @Test
    fun should_fail_at_startup_when_same_site_none_is_not_secure() {
        assertThrows<IllegalArgumentException> { AuthCookieProperties(false, AuthCookieProperties.SameSite.NONE) }
        assertEquals("None", AuthCookieProperties(true, AuthCookieProperties.SameSite.NONE).sameSite.attribute)
    }
}
