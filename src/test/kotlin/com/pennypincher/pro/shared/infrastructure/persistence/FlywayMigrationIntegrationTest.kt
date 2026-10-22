package com.pennypincher.pro.shared.infrastructure.persistence

import com.pennypincher.pro.support.IntegrationTest
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate

class FlywayMigrationIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun should_apply_all_eleven_migrations_when_application_starts() {
        val versions =
            jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String::class.java,
            )

        assertEquals((1..MIGRATION_COUNT).map(Int::toString), versions)
    }

    @Test
    fun should_create_every_domain_table_when_migrations_are_applied() {
        val tables =
            jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String::class.java,
            )

        assertTrue(tables.containsAll(EXPECTED_TABLES + FLYWAY_HISTORY_TABLE)) { "Tables found: $tables" }
    }

    @Test
    fun should_store_money_as_numeric_18_2_when_column_is_monetary() {
        val wrongColumns =
            jdbc.queryForList(
                """
                SELECT table_name || '.' || column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND data_type = 'numeric'
                  AND column_name NOT LIKE '%percentage' AND column_name <> 'expected_annual_return'
                  AND (numeric_precision <> 18 OR numeric_scale <> 2)
                """.trimIndent(),
                String::class.java,
            )

        assertEquals(emptyList<String>(), wrongColumns)
    }

    @Test
    fun should_default_primary_keys_to_gen_random_uuid_when_table_is_created() {
        val defaults =
            jdbc.queryForList(
                "SELECT table_name, column_default FROM information_schema.columns " +
                    "WHERE table_schema = 'public' AND column_name = 'id' AND data_type = 'uuid'",
            )

        assertEquals(EXPECTED_TABLES.toSet(), defaults.map { it["table_name"] }.toSet())
        defaults.forEach { assertEquals("gen_random_uuid()", it["column_default"]) }
    }

    @Test
    fun should_use_timestamptz_when_column_is_an_audit_timestamp() {
        val wrongColumns =
            jdbc.queryForList(
                """
                SELECT table_name || '.' || column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND column_name LIKE '%\_at'
                  AND table_name <> 'flyway_schema_history' AND data_type <> 'timestamp with time zone'
                """.trimIndent(),
                String::class.java,
            )

        assertEquals(emptyList<String>(), wrongColumns)
    }

    @Test
    fun should_index_user_id_when_table_references_users() {
        val unindexed =
            jdbc.queryForList(
                """
                SELECT c.relname FROM pg_attribute a
                JOIN pg_class c ON c.oid = a.attrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace AND n.nspname = 'public'
                WHERE a.attname = 'user_id' AND c.relkind = 'r'
                  AND NOT EXISTS (SELECT 1 FROM pg_index i WHERE i.indrelid = c.oid AND i.indkey[0] = a.attnum)
                """.trimIndent(),
                String::class.java,
            )

        assertEquals(emptyList<String>(), unindexed)
    }

    @Test
    fun should_not_use_fixed_length_char_columns_when_table_is_created() {
        val charColumns =
            jdbc.queryForList(
                "SELECT table_name || '.' || column_name FROM information_schema.columns " +
                    "WHERE table_schema = 'public' AND data_type = 'character'",
                String::class.java,
            )

        assertEquals(emptyList<String>(), charColumns)
    }

    @Test
    fun should_refresh_updated_at_when_row_is_updated() {
        val userId = insertUser()
        val before = updatedAtOf(userId)

        jdbc.update("UPDATE users SET name = 'Renamed' WHERE id = ?", userId)

        assertNotEquals(before, updatedAtOf(userId))
    }

    @Test
    fun should_reject_second_budget_when_same_user_and_month_already_exist() {
        val userId = insertUser()
        val insertBudget = "INSERT INTO monthly_budgets (user_id, year, month) VALUES (?, 2026, 10)"
        jdbc.update(insertBudget, userId)

        assertThrows<DataIntegrityViolationException> { jdbc.update(insertBudget, userId) }
    }

    private fun insertUser(): UUID {
        val suffix = UUID.randomUUID()
        return jdbc
            .queryForList(
                "INSERT INTO users (email, name, google_sub) VALUES (?, 'Test', ?) RETURNING id",
                UUID::class.java,
                "user-$suffix@example.com",
                "sub-$suffix",
            ).single()
    }

    private fun updatedAtOf(userId: UUID): Any? =
        jdbc.queryForObject("SELECT updated_at FROM users WHERE id = ?", Any::class.java, userId)

    private companion object {
        const val MIGRATION_COUNT = 11
        const val FLYWAY_HISTORY_TABLE = "flyway_schema_history"
        val EXPECTED_TABLES =
            listOf(
                "users", "refresh_tokens", "assets", "liabilities", "net_worth_snapshots",
                "income_sources", "monthly_budgets", "credit_cards", "loans",
                "fixed_expenses", "variable_expenses", "savings_accounts", "financial_goals",
            )
    }
}
