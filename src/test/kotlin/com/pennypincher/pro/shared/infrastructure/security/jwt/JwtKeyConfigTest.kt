package com.pennypincher.pro.shared.infrastructure.security.jwt

import com.pennypincher.pro.support.TestRsaKeys
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class JwtKeyConfigTest {
    private val config = JwtKeyConfig()

    @Test
    fun should_load_configured_keys_when_they_are_base64_encoded_pem() {
        val (privatePem, publicPem) = TestRsaKeys.base64Pem(TestRsaKeys.applicationKeys)

        val keys = config.jwtSigningKeys(properties(privatePem, publicPem))

        assertEquals(TestRsaKeys.applicationKeys, keys)
    }

    @Test
    fun should_load_configured_keys_when_they_are_plain_pem() {
        val source = TestRsaKeys.applicationKeys
        val keys =
            config.jwtSigningKeys(properties(PemRsaKeys.toPem(source.privateKey), PemRsaKeys.toPem(source.publicKey)))

        assertEquals(source, keys)
    }

    @Test
    fun should_fail_at_startup_when_keys_are_missing_and_generation_is_off() {
        assertThrows<IllegalStateException> { config.jwtSigningKeys(properties("", "")) }
    }

    @Test
    fun should_report_missing_keys_when_environment_placeholders_are_unresolved() {
        val exception = assertThrows<IllegalStateException> {
            config.jwtSigningKeys(properties("\${JWT_PRIVATE_KEY_PEM}", "\${JWT_PUBLIC_KEY_PEM}"))
        }

        assertEquals("jwt.private-key and jwt.public-key are required", exception.message)
    }

    @Test
    fun should_fail_at_startup_when_key_is_not_pem() {
        assertThrows<IllegalArgumentException> { config.jwtSigningKeys(properties("not a key!", "not a key!")) }
        assertThrows<IllegalArgumentException> {
            PemRsaKeys.parsePublicKey(PemRsaKeys.toPem(TestRsaKeys.applicationKeys.privateKey))
        }
    }

    @Test
    fun should_generate_once_and_reuse_keys_when_generation_is_on(@TempDir directory: Path) {
        val keyDir = directory.resolve("keys")
        val generated = config.jwtSigningKeys(properties("", "", generate = true, keyDir = keyDir))

        val reloaded = config.jwtSigningKeys(properties("", "", generate = true, keyDir = keyDir))

        assertEquals(generated, reloaded)
        assertTrue(Files.exists(keyDir.resolve("jwt-private.pem")))
    }

    private fun properties(privateKey: String, publicKey: String, generate: Boolean = false, keyDir: Path? = null) =
        JwtProperties(
            issuer = "pennypincher-pro",
            accessTokenTtl = Duration.ofMinutes(15),
            refreshTokenTtl = Duration.ofDays(30),
            privateKey = privateKey,
            publicKey = publicKey,
            generateMissingKeys = generate,
            localKeyDir = keyDir?.toString() ?: "build/unused",
        )
}
