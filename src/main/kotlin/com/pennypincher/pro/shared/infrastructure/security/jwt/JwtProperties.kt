package com.pennypincher.pro.shared.infrastructure.security.jwt

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Settings of the application's own tokens, bound from `jwt.*`. The PEM keys may be plain PEM text or
 * Base64-encoded PEM. When they are blank and `generate-missing-keys` is on (local and test profiles),
 * a key pair is generated once and kept in `local-key-dir`.
 */
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val issuer: String,
    val accessTokenTtl: Duration,
    val refreshTokenTtl: Duration,
    val privateKey: String = "",
    val publicKey: String = "",
    val generateMissingKeys: Boolean = false,
    val localKeyDir: String = "build/keys",
)
