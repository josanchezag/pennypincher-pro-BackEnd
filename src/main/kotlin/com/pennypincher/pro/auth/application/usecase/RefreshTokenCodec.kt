package com.pennypincher.pro.auth.application.usecase

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.HexFormat

/** Generates opaque refresh token values and the SHA-256 hash under which they are stored. */
class RefreshTokenCodec(private val random: SecureRandom = SecureRandom()) {
    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance(HASH_ALGORITHM).digest(token.toByteArray(Charsets.UTF_8)))

    private companion object {
        const val TOKEN_BYTES = 32
        const val HASH_ALGORITHM = "SHA-256"
    }
}
