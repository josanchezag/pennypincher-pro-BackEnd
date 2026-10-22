package com.pennypincher.pro.support

import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtSigningKeys
import com.pennypincher.pro.shared.infrastructure.security.jwt.PemRsaKeys
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

/** RSA key pairs generated on the fly for tests; nothing is read from disk or the network. */
object TestRsaKeys {
    private const val KEY_SIZE_BITS = 2048

    val applicationKeys: JwtSigningKeys by lazy { generate() }

    fun generate(): JwtSigningKeys {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(KEY_SIZE_BITS) }.generateKeyPair()
        return JwtSigningKeys(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey)
    }

    fun base64Pem(keys: JwtSigningKeys): Pair<String, String> =
        encode(PemRsaKeys.toPem(keys.privateKey)) to encode(PemRsaKeys.toPem(keys.publicKey))

    private fun encode(pem: String): String = Base64.getEncoder().encodeToString(pem.toByteArray())
}
