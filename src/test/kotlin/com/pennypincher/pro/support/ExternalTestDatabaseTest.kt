package com.pennypincher.pro.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExternalTestDatabaseTest {
    @Test
    fun should_return_null_when_test_db_url_is_missing() {
        assertNull(ExternalTestDatabase.fromEnvironment(emptyMap()))
    }

    @Test
    fun should_read_connection_data_when_test_db_url_is_set() {
        val database =
            ExternalTestDatabase.fromEnvironment(
                mapOf(
                    ExternalTestDatabase.URL_VARIABLE to "jdbc:postgresql://localhost:5432/test",
                    ExternalTestDatabase.USERNAME_VARIABLE to "user",
                    ExternalTestDatabase.PASSWORD_VARIABLE to "secret",
                ),
            )

        assertEquals("jdbc:postgresql://localhost:5432/test", database?.jdbcUrl)
        assertEquals("user", database?.username)
        assertEquals("secret", database?.password)
    }
}
