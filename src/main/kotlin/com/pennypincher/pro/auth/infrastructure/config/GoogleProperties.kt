package com.pennypincher.pro.auth.infrastructure.config

import com.pennypincher.pro.shared.infrastructure.config.ConfigValues
import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Google OAuth2 web client settings, bound from `google.*`. `allowed-redirect-uris` lists the exact
 * redirect URIs the frontend may use (environment variable `GOOGLE_ALLOWED_REDIRECT_URIS`).
 * With `credentials-required` (the `prod` profile) a missing client id or secret stops the startup;
 * locally they may stay empty because dev-login works without Google.
 */
@ConfigurationProperties(prefix = "google")
data class GoogleProperties(
    val clientId: String,
    val clientSecret: String,
    val tokenUri: String,
    val jwkSetUri: String,
    val allowedRedirectUris: List<String>,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    val credentialsRequired: Boolean = false,
) {
    init {
        if (credentialsRequired) {
            ConfigValues.requireProvided("google.client-id", clientId)
            ConfigValues.requireProvided("google.client-secret", clientSecret)
        }
        require(!ConfigValues.isUnresolvedPlaceholder(clientId + clientSecret)) {
            "google.client-id and google.client-secret must not be unresolved placeholders"
        }
        require(allowedRedirectUris.isNotEmpty()) { "google.allowed-redirect-uris is required" }
        allowedRedirectUris.forEach { ConfigValues.requireProvided("google.allowed-redirect-uris", it) }
    }
}
