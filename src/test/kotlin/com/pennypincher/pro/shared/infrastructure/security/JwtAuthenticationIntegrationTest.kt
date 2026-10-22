package com.pennypincher.pro.shared.infrastructure.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.pennypincher.pro.auth.domain.port.outbound.AccessTokenIssuer
import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtSigningKeys
import com.pennypincher.pro.support.AuthFixtures
import com.pennypincher.pro.support.IntegrationTest
import com.pennypincher.pro.support.PrincipalProbeController
import com.pennypincher.pro.support.TestRsaKeys
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class JwtAuthenticationIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var accessTokenIssuer: AccessTokenIssuer

    @Autowired
    private lateinit var signingKeys: JwtSigningKeys

    @Test
    fun should_resolve_principal_when_access_token_is_valid() {
        val user = AuthFixtures.user()

        probe(accessTokenIssuer.issue(user).value).andExpect {
            status { isOk() }
            jsonPath("$.userId") { value(user.id.toString()) }
            jsonPath("$.email") { value(user.email) }
        }
    }

    @Test
    fun should_return_unauthorized_envelope_when_token_is_missing() {
        mockMvc.get(PrincipalProbeController.PATH).andExpect {
            status { isUnauthorized() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.error.code") { value("UNAUTHORIZED") }
            jsonPath("$.meta.requestId") { isNotEmpty() }
            header { string(HttpHeaders.WWW_AUTHENTICATE, "Bearer") }
        }
    }

    @Test
    fun should_return_unauthorized_envelope_when_token_is_expired() {
        val expired = sign(claims(issuedAt = Instant.now().minus(Duration.ofHours(1))))

        probe(expired).andExpectUnauthorized()
    }

    @Test
    fun should_return_unauthorized_when_token_is_signed_by_another_key() {
        probe(sign(claims(), TestRsaKeys.generate())).andExpectUnauthorized()
    }

    @Test
    fun should_return_unauthorized_when_issuer_or_subject_is_wrong() {
        probe(sign(claims(issuer = "someone-else"))).andExpectUnauthorized()
        probe(sign(claims(subject = "not-a-uuid"))).andExpectUnauthorized()
        probe(sign(claims(email = null))).andExpectUnauthorized()
    }

    @Test
    fun should_return_unauthorized_when_token_is_unsigned_with_alg_none() {
        val header = base64Url("""{"alg":"none","typ":"JWT"}""")
        val now = Instant.now().epochSecond
        val payload =
            base64Url(
                """{"iss":"pennypincher-pro","sub":"${UUID.randomUUID()}","email":"a@b.c","iat":$now,"exp":${now + 600}}""",
            )

        probe("$header.$payload.").andExpectUnauthorized()
    }

    @Test
    fun should_return_unauthorized_when_token_is_hs256_signed_with_the_public_key() {
        val claims = JWTClaimsSet.Builder()
            .issuer("pennypincher-pro")
            .subject(UUID.randomUUID().toString())
            .claim("email", "a@b.c")
            .issueTime(Date())
            .expirationTime(Date.from(Instant.now().plus(Duration.ofMinutes(10))))
            .build()
        val forged = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims).apply {
            sign(MACSigner(signingKeys.publicKey.encoded))
        }

        probe(forged.serialize()).andExpectUnauthorized()
    }

    @Test
    fun should_return_unauthorized_when_token_is_garbage() {
        probe("garbage").andExpectUnauthorized()
    }

    @Test
    fun should_ignore_stale_bearer_token_when_calling_public_auth_endpoint() {
        mockMvc.post("/api/v1/auth/google/callback") {
            header(HttpHeaders.AUTHORIZATION, "Bearer garbage")
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_FAILED") }
        }
    }

    @Test
    fun should_return_not_found_envelope_when_authenticated_route_does_not_exist() {
        mockMvc.get("/api/v1/does-not-exist") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${accessTokenIssuer.issue(AuthFixtures.user()).value}")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") { value("NOT_FOUND") }
        }
    }

    private fun base64Url(json: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())

    private fun probe(token: String): ResultActionsDsl = mockMvc.get(PrincipalProbeController.PATH) {
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    private fun ResultActionsDsl.andExpectUnauthorized() = andExpect {
        status { isUnauthorized() }
        jsonPath("$.error.code") { value("UNAUTHORIZED") }
    }

    private fun claims(
        issuer: String = "pennypincher-pro",
        subject: String = UUID.randomUUID().toString(),
        email: String? = "ana@example.com",
        issuedAt: Instant = Instant.now(),
    ): JwtClaimsSet = JwtClaimsSet.builder()
        .issuer(issuer)
        .subject(subject)
        .issuedAt(issuedAt)
        .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
        .apply { email?.let { claim("email", it) } }
        .build()

    private fun sign(claims: JwtClaimsSet, keys: JwtSigningKeys = signingKeys): String {
        val jwk = RSAKey.Builder(keys.publicKey).privateKey(keys.privateKey).build()
        val encoder = NimbusJwtEncoder(ImmutableJWKSet<SecurityContext>(JWKSet(jwk)))
        return encoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims),
        ).tokenValue
    }
}
