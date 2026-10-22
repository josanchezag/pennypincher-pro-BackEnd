package com.pennypincher.pro.auth.infrastructure.web

import com.pennypincher.pro.auth.infrastructure.config.AuthCookieProperties
import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtProperties
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import java.time.Duration
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

/** Builds the `ppp_refresh` cookie that carries the refresh token, and the cookie that clears it. */
@Component
class RefreshCookieFactory(private val properties: AuthCookieProperties, jwtProperties: JwtProperties) {
    private val maxAge: Duration = jwtProperties.refreshTokenTtl

    fun create(refreshToken: String): ResponseCookie = baseCookie(refreshToken).maxAge(maxAge).build()

    fun clear(): ResponseCookie = baseCookie("").maxAge(Duration.ZERO).build()

    private fun baseCookie(value: String): ResponseCookie.ResponseCookieBuilder =
        ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(properties.secure)
            .sameSite(properties.sameSite.attribute)
            .path(ApiPaths.AUTH)

    companion object {
        const val COOKIE_NAME = "ppp_refresh"
    }
}
