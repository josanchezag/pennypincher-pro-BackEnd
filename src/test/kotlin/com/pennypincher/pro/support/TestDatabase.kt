package com.pennypincher.pro.support

/** Connection data of the PostgreSQL 16 database used by integration tests. */
interface TestDatabase {
    val jdbcUrl: String
    val username: String
    val password: String
}
