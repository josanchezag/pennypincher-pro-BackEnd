package com.pennypincher.pro.shared.infrastructure.web.filter

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class RequestIdFilterTest {
    @Test
    fun should_keep_incoming_id_when_it_is_a_uuid() {
        val id = "3f2b8c1e-9d4a-4f6b-8e2a-1c5d7e9f0a3b"

        assertEquals(id, RequestIdFilter.resolve(" $id "))
    }

    @Test
    fun should_generate_new_id_when_incoming_value_is_missing_or_not_a_uuid() {
        listOf(null, "", "abc", "1-1-1-1-1", "<script>").forEach { candidate ->
            val resolved = RequestIdFilter.resolve(candidate)

            assertNotEquals(candidate, resolved)
            UUID.fromString(resolved)
        }
    }
}
