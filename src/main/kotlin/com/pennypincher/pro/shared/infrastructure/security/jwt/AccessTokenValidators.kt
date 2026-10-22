package com.pennypincher.pro.shared.infrastructure.security.jwt

import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtClaimRules.rule
import java.util.UUID
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators

/** Checks the application's access tokens: issuer, expiry, a UUID subject and an e-mail claim. */
object AccessTokenValidators {
    fun create(issuer: String): OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(
        JwtValidators.createDefaultWithIssuer(issuer),
        rule("Access token subject must be a user id") { jwt -> jwt.subject?.let(::isUuid) == true },
        rule("Access token e-mail claim is missing") { jwt ->
            !jwt.getClaimAsString(AccessTokenClaims.EMAIL).isNullOrBlank()
        },
    )

    private fun isUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess
}
