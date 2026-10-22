package com.pennypincher.pro.auth.infrastructure.google

import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtClaimRules.rule
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtTimestampValidator

/** Rules a Google ID token must satisfy besides a valid signature. */
object GoogleIdTokenValidators {
    val ISSUERS = setOf("accounts.google.com", "https://accounts.google.com")
    const val EMAIL_CLAIM = "email"
    const val EMAIL_VERIFIED_CLAIM = "email_verified"

    fun create(clientId: String): OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(
        JwtTimestampValidator(),
        rule("Google ID token issuer is not Google") { it.getClaimAsString(JwtClaimNames.ISS) in ISSUERS },
        rule("Google ID token audience does not match the client id") { clientId in it.audience.orEmpty() },
        rule("Google ID token subject is missing") { !it.subject.isNullOrBlank() },
        rule("Google ID token e-mail is missing") { !it.getClaimAsString(EMAIL_CLAIM).isNullOrBlank() },
        rule("Google ID token e-mail is not verified") { it.getClaimAsBoolean(EMAIL_VERIFIED_CLAIM) == true },
    )
}
