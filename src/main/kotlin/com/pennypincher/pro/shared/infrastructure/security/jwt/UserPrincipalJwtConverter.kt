package com.pennypincher.pro.shared.infrastructure.security.jwt

import com.pennypincher.pro.shared.domain.model.UserPrincipal
import java.util.UUID
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt

/** Turns a validated access token into an authentication whose principal is a [UserPrincipal]. */
class UserPrincipalJwtConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val principal = UserPrincipal(UUID.fromString(jwt.subject), jwt.getClaimAsString(AccessTokenClaims.EMAIL))
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, emptyList())
    }
}
