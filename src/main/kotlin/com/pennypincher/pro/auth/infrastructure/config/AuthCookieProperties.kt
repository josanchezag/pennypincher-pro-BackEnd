package com.pennypincher.pro.auth.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Attributes of the refresh token cookie, bound from `auth.cookie.*` (`AUTH_COOKIE_SECURE`,
 * `AUTH_COOKIE_SAME_SITE`). Browsers reject `SameSite=None` without `Secure`, so that combination
 * stops the application at startup.
 */
@ConfigurationProperties(prefix = "auth.cookie")
data class AuthCookieProperties(val secure: Boolean = true, val sameSite: SameSite = SameSite.STRICT) {
    init {
        require(sameSite != SameSite.NONE || secure) { "auth.cookie.same-site=None requires auth.cookie.secure=true" }
    }

    enum class SameSite(val attribute: String) {
        STRICT("Strict"),
        LAX("Lax"),
        NONE("None"),
    }
}
