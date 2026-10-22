package com.pennypincher.pro.auth.infrastructure.google

import com.pennypincher.pro.auth.domain.exception.GoogleAuthenticationException
import com.pennypincher.pro.auth.domain.exception.IdentityProviderUnavailableException
import com.pennypincher.pro.auth.domain.model.VerifiedIdentity
import com.pennypincher.pro.user.domain.model.IdentityProfile
import com.pennypincher.pro.user.domain.model.User
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException

/**
 * Verifies a Google ID token with a decoder that checks the signature against Google's JWKS and
 * applies [GoogleIdTokenValidators], then extracts the identity.
 */
class GoogleIdTokenVerifier(private val decoder: JwtDecoder) {
    fun verify(idToken: String): VerifiedIdentity {
        val jwt = try {
            decoder.decode(idToken)
        } catch (exception: BadJwtException) {
            throw GoogleAuthenticationException("Google ID token is invalid", exception)
        } catch (exception: JwtException) {
            throw IdentityProviderUnavailableException("Google signing keys could not be retrieved", exception)
        }
        return VerifiedIdentity(jwt.subject, profileOf(jwt))
    }

    private fun profileOf(jwt: Jwt): IdentityProfile {
        val email = jwt.getClaimAsString(GoogleIdTokenValidators.EMAIL_CLAIM)
        val name = jwt.getClaimAsString(NAME_CLAIM)?.trim()?.takeIf(String::isNotEmpty) ?: email
        return IdentityProfile(email, name.take(User.MAX_NAME_LENGTH), jwt.getClaimAsString(PICTURE_CLAIM))
    }

    private companion object {
        const val NAME_CLAIM = "name"
        const val PICTURE_CLAIM = "picture"
    }
}
