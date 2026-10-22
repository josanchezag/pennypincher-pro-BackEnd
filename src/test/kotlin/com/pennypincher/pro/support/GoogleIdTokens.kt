package com.pennypincher.pro.support

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtSigningKeys
import java.time.Duration
import java.time.Instant
import java.util.Date

/** Signs fake Google ID tokens with a locally generated key that plays the role of Google's JWKS. */
class GoogleIdTokens(private val keys: JwtSigningKeys = TestRsaKeys.generate()) {
    val publicKey get() = keys.publicKey

    val jwkSetJson: String
        get() = JWKSet(RSAKey.Builder(keys.publicKey).keyID(KEY_ID).build()).toString()

    fun sign(claims: Claims = Claims()): String {
        val builder = JWTClaimsSet.Builder()
            .issuer(claims.issuer)
            .audience(claims.audience)
            .subject(claims.subject)
            .issueTime(Date.from(claims.issuedAt))
            .expirationTime(Date.from(claims.issuedAt.plus(claims.lifetime)))
            .claim("email", claims.email)
            .claim("email_verified", claims.emailVerified)
            .claim("name", claims.name)
            .claim("picture", claims.picture)
        val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), builder.build())
        jwt.sign(RSASSASigner(keys.privateKey))
        return jwt.serialize()
    }

    data class Claims(
        val issuer: String = "https://accounts.google.com",
        val audience: String = CLIENT_ID,
        val subject: String? = "google-sub-123",
        val email: String? = "ana@example.com",
        val emailVerified: Any? = true,
        val name: String? = "Ana Pérez",
        val picture: String? = "https://lh3.googleusercontent.com/a/ana",
        val issuedAt: Instant = Instant.now(),
        val lifetime: Duration = Duration.ofHours(1),
    )

    companion object {
        const val CLIENT_ID = "test-client-id"
        private const val KEY_ID = "google-test-key"
    }
}
