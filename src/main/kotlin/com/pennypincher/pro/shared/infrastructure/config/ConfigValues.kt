package com.pennypincher.pro.shared.infrastructure.config

/**
 * Checks for configuration values that come from environment variables. Spring leaves an
 * unresolvable `${VARIABLE}` placeholder as literal text when binding properties, so a missing
 * variable must be detected explicitly to fail at startup.
 */
object ConfigValues {
    private const val PLACEHOLDER_MARKER = "\${"

    fun isUnresolvedPlaceholder(value: String): Boolean = PLACEHOLDER_MARKER in value

    /** True when [value] holds real content: not blank and not an unresolved placeholder. */
    fun isProvided(value: String): Boolean = value.isNotBlank() && !isUnresolvedPlaceholder(value)

    /** Fails with a message naming [property] when [value] is blank or an unresolved placeholder. */
    fun requireProvided(property: String, value: String): String {
        require(isProvided(value)) { "$property is required (set its environment variable)" }
        return value
    }
}
