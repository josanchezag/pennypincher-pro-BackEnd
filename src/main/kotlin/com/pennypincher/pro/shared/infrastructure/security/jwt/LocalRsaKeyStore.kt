package com.pennypincher.pro.shared.infrastructure.security.jwt

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import org.slf4j.LoggerFactory

/**
 * Development key store: reuses the PEM files in [directory] or generates a new RSA key pair there,
 * so local tokens survive restarts. Never used when keys are configured explicitly.
 */
class LocalRsaKeyStore(private val directory: Path) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val privateKeyFile = directory.resolve(PRIVATE_KEY_FILE)
    private val publicKeyFile = directory.resolve(PUBLIC_KEY_FILE)

    fun loadOrCreate(): JwtSigningKeys = if (Files.exists(privateKeyFile) && Files.exists(publicKeyFile)) {
        JwtSigningKeys(
            publicKey = PemRsaKeys.parsePublicKey(Files.readString(publicKeyFile)),
            privateKey = PemRsaKeys.parsePrivateKey(Files.readString(privateKeyFile)),
        )
    } else {
        create()
    }

    private fun create(): JwtSigningKeys {
        val keyPair = KeyPairGenerator.getInstance(ALGORITHM).apply { initialize(KEY_SIZE_BITS) }.generateKeyPair()
        val keys = JwtSigningKeys(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey)
        Files.createDirectories(directory)
        Files.writeString(privateKeyFile, PemRsaKeys.toPem(keys.privateKey))
        Files.writeString(publicKeyFile, PemRsaKeys.toPem(keys.publicKey))
        restrictToOwner(privateKeyFile)
        logger.info("Generated a development RSA key pair in {}", directory.toAbsolutePath())
        return keys
    }

    private fun restrictToOwner(file: Path) {
        if (Files.getFileStore(file).supportsFileAttributeView(POSIX_VIEW)) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(OWNER_ONLY))
        }
    }

    private companion object {
        const val PRIVATE_KEY_FILE = "jwt-private.pem"
        const val PUBLIC_KEY_FILE = "jwt-public.pem"
        const val ALGORITHM = "RSA"
        const val KEY_SIZE_BITS = 2048
        const val POSIX_VIEW = "posix"
        const val OWNER_ONLY = "rw-------"
    }
}
