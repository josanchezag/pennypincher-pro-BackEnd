package com.pennypincher.pro.support

/**
 * Chooses the integration-test database: an external one when `TEST_DB_URL` is set,
 * otherwise a Testcontainers PostgreSQL instance shared by every test class.
 */
object TestDatabases {
    val current: TestDatabase by lazy { ExternalTestDatabase.fromEnvironment() ?: ContainerTestDatabase() }
}
