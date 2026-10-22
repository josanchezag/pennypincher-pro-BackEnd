package com.pennypincher.pro.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/** Base class for tests that boot the whole application against a real PostgreSQL database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { TestDatabases.current.jdbcUrl }
            registry.add("spring.datasource.username") { TestDatabases.current.username }
            registry.add("spring.datasource.password") { TestDatabases.current.password }
        }
    }
}
