package com.pennypincher.pro.auth.infrastructure.config

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Refresh rotation settings, bound from `auth.refresh.*`. `reuse-grace` is how long a just-rotated
 * token is treated as a concurrent refresh (401 without logging the user out) instead of a replay.
 */
@ConfigurationProperties(prefix = "auth.refresh")
data class RefreshTokenProperties(val reuseGrace: Duration = DEFAULT_REUSE_GRACE) {
    companion object {
        val DEFAULT_REUSE_GRACE: Duration = Duration.ofSeconds(30)
    }
}
