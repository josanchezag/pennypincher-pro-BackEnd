package com.pennypincher.pro.auth.infrastructure.security

import com.pennypincher.pro.auth.domain.model.AccessToken
import com.pennypincher.pro.auth.domain.port.outbound.AccessTokenIssuer
import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtKeyConfig
import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtProperties
import com.pennypincher.pro.user.domain.model.User
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component

/** Signs RS256 access tokens with the claims `sub` (user id), `email`, `iss`, `iat` and `exp`. */
@Component
class JwtAccessTokenIssuer(
    private val encoder: JwtEncoder,
    private val properties: JwtProperties,
    private val clock: Clock,
) : AccessTokenIssuer {
    override fun issue(user: User): AccessToken {
        val issuedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(user.id.toString())
            .claim(EMAIL_CLAIM, user.email)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(properties.accessTokenTtl))
            .build()
        val header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(JwtKeyConfig.KEY_ID).build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return AccessToken(token, properties.accessTokenTtl)
    }

    companion object {
        const val EMAIL_CLAIM = "email"
    }
}
