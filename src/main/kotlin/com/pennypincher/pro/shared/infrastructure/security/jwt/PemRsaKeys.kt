package com.pennypincher.pro.shared.infrastructure.security.jwt

import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Reads and writes RSA keys as PEM (PKCS#8 private key, X.509 public key). */
object PemRsaKeys {
    private const val PRIVATE_LABEL = "PRIVATE KEY"
    private const val PUBLIC_LABEL = "PUBLIC KEY"
    private const val PEM_LINE_LENGTH = 64
    private const val PEM_PREFIX = "-----BEGIN"
    private const val ALGORITHM = "RSA"
    private val WHITESPACE = Regex("\\s")

    fun parsePrivateKey(value: String): RSAPrivateKey =
        keyFactory().generatePrivate(PKCS8EncodedKeySpec(decodeBody(value, PRIVATE_LABEL))) as RSAPrivateKey

    fun parsePublicKey(value: String): RSAPublicKey =
        keyFactory().generatePublic(X509EncodedKeySpec(decodeBody(value, PUBLIC_LABEL))) as RSAPublicKey

    fun toPem(privateKey: RSAPrivateKey): String = encode(privateKey.encoded, PRIVATE_LABEL)

    fun toPem(publicKey: RSAPublicKey): String = encode(publicKey.encoded, PUBLIC_LABEL)

    private fun decodeBody(value: String, label: String): ByteArray {
        val pem = toPemText(value)
        val header = "-----BEGIN $label-----"
        val footer = "-----END $label-----"
        require(header in pem && footer in pem) { "Expected a PEM block of type '$label'" }
        return base64(pem.substringAfter(header).substringBefore(footer))
    }

    private fun toPemText(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith(PEM_PREFIX)) trimmed else String(base64(trimmed), Charsets.UTF_8)
    }

    private fun base64(value: String): ByteArray = try {
        Base64.getDecoder().decode(value.replace(WHITESPACE, ""))
    } catch (exception: IllegalArgumentException) {
        throw IllegalArgumentException("RSA key is neither PEM nor Base64-encoded PEM", exception)
    }

    private fun encode(der: ByteArray, label: String): String {
        val body = Base64.getMimeEncoder(PEM_LINE_LENGTH, "\n".toByteArray()).encodeToString(der)
        return "-----BEGIN $label-----\n$body\n-----END $label-----\n"
    }

    private fun keyFactory(): KeyFactory = KeyFactory.getInstance(ALGORITHM)
}
