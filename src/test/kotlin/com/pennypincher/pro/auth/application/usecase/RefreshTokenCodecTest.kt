package com.pennypincher.pro.auth.application.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RefreshTokenCodecTest {
    private val codec = RefreshTokenCodec()

    @Test
    fun should_generate_unique_url_safe_256_bit_values() {
        val first = codec.generate()

        assertEquals(43, first.length)
        assertTrue(first.matches(Regex("[A-Za-z0-9_-]+")))
        assertNotEquals(first, codec.generate())
    }

    @Test
    fun should_hash_with_sha256_hex_when_hashing_a_token() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", codec.hash("abc"))
    }
}
