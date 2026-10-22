package com.pennypincher.pro.support

/** An already running database, configured through `TEST_DB_URL`, `TEST_DB_USERNAME` and `TEST_DB_PASSWORD`. */
class ExternalTestDatabase(
    override val jdbcUrl: String,
    override val username: String,
    override val password: String,
) : TestDatabase {
    companion object {
        const val URL_VARIABLE = "TEST_DB_URL"
        const val USERNAME_VARIABLE = "TEST_DB_USERNAME"
        const val PASSWORD_VARIABLE = "TEST_DB_PASSWORD"
        private const val DEFAULT_CREDENTIAL = "postgres"

        fun fromEnvironment(environment: Map<String, String> = System.getenv()): ExternalTestDatabase? {
            val url = environment[URL_VARIABLE]?.takeIf(String::isNotBlank) ?: return null
            return ExternalTestDatabase(
                jdbcUrl = url,
                username = environment[USERNAME_VARIABLE] ?: DEFAULT_CREDENTIAL,
                password = environment[PASSWORD_VARIABLE] ?: DEFAULT_CREDENTIAL,
            )
        }
    }
}
