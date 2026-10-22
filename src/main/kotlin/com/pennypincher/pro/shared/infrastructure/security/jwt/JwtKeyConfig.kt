package com.pennypincher.pro.shared.infrastructure.security.jwt

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import com.pennypincher.pro.shared.infrastructure.config.ConfigValues
import java.nio.file.Path
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder

/** Loads the RSA signing keys and exposes the RS256 encoder used to sign access tokens. */
@Configuration
class JwtKeyConfig {
    @Bean
    fun jwtSigningKeys(properties: JwtProperties): JwtSigningKeys {
        val configured = ConfigValues.isProvided(properties.privateKey) && ConfigValues.isProvided(properties.publicKey)
        return when {
            configured -> JwtSigningKeys(
                publicKey = PemRsaKeys.parsePublicKey(properties.publicKey),
                privateKey = PemRsaKeys.parsePrivateKey(properties.privateKey),
            )
            properties.generateMissingKeys -> LocalRsaKeyStore(Path.of(properties.localKeyDir)).loadOrCreate()
            else -> throw IllegalStateException("jwt.private-key and jwt.public-key are required")
        }
    }

    @Bean
    fun jwtEncoder(keys: JwtSigningKeys): JwtEncoder {
        val jwk = RSAKey.Builder(keys.publicKey).privateKey(keys.privateKey).keyID(KEY_ID).build()
        return NimbusJwtEncoder(ImmutableJWKSet<SecurityContext>(JWKSet(jwk)))
    }

    companion object {
        const val KEY_ID = "pennypincher-pro-access"
    }
}
