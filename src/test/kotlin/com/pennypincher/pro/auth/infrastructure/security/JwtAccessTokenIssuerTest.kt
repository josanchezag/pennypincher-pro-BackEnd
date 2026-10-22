package com.pennypincher.pro.auth.infrastructure.security

import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtKeyConfig
import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtProperties
import com.pennypincher.pro.support.AuthFixtures
import com.pennypincher.pro.support.TestRsaKeys
import java.time.Clock
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

class JwtAccessTokenIssuerTest {
    private val keys = TestRsaKeys.applicationKeys
    private val properties = JwtProperties("pennypincher-pro", Duration.ofMinutes(15), Duration.ofDays(30))
    private val issuer = JwtAccessTokenIssuer(JwtKeyConfig().jwtEncoder(keys), properties, Clock.systemUTC())

    @Test
    fun should_sign_rs256_token_with_expected_claims_when_issuing() {
        val user = AuthFixtures.user()

        val token = issuer.issue(user)
        val jwt = NimbusJwtDecoder.withPublicKey(keys.publicKey).build().decode(token.value)

        assertEquals(Duration.ofMinutes(15), token.expiresIn)
        assertEquals(user.id.toString(), jwt.subject)
        assertEquals(user.email, jwt.getClaimAsString("email"))
        assertEquals("pennypincher-pro", jwt.getClaimAsString("iss"))
        assertEquals("RS256", jwt.headers["alg"].toString())
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.issuedAt, jwt.expiresAt))
        assertEquals(true, jwt.expiresAt!!.isAfter(Instant.now()))
    }
}
