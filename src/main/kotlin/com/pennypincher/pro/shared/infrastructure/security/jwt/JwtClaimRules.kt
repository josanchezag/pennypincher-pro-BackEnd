package com.pennypincher.pro.shared.infrastructure.security.jwt

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/** Builds single-purpose JWT validators from a predicate and the reason reported when it fails. */
object JwtClaimRules {
    fun rule(failureDescription: String, isValid: (Jwt) -> Boolean) = OAuth2TokenValidator<Jwt> { jwt ->
        if (isValid(jwt)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, failureDescription, null))
        }
    }
}
