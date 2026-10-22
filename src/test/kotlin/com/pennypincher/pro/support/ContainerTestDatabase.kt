package com.pennypincher.pro.support

import org.testcontainers.containers.PostgreSQLContainer

/** A throwaway PostgreSQL 16 container started once per test JVM. */
class ContainerTestDatabase : TestDatabase {
    private val container =
        PostgreSQLContainer(IMAGE).apply {
            withDatabaseName(DATABASE_NAME)
            start()
        }

    override val jdbcUrl: String get() = container.jdbcUrl
    override val username: String get() = container.username
    override val password: String get() = container.password

    private companion object {
        const val IMAGE = "postgres:16-alpine"
        const val DATABASE_NAME = "pennypincher_test"
    }
}
