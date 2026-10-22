package com.pennypincher.pro.shared.infrastructure.config

import java.net.URI
import java.net.URISyntaxException

/**
 * Checks that every configured CORS origin is an absolute http(s) origin: scheme, host and an
 * optional port, without path, query, wildcard or an unresolved `${...}` placeholder.
 */
object CorsOriginValidator {
    private val ALLOWED_SCHEMES = setOf("http", "https")
    private const val PLACEHOLDER_MARKER = "\${"
    private const val WILDCARD = "*"

    fun validate(origins: List<String>): List<String> {
        val cleanOrigins = origins.map(String::trim).filter(String::isNotEmpty)
        require(cleanOrigins.isNotEmpty()) { "cors.allowed-origins must list at least one origin" }
        cleanOrigins.forEach(::requireValidOrigin)
        return cleanOrigins
    }

    private fun requireValidOrigin(origin: String) {
        require(PLACEHOLDER_MARKER !in origin) { "cors.allowed-origins has an unresolved placeholder: '$origin'" }
        require(WILDCARD !in origin) { "cors.allowed-origins cannot use '*' because credentials are allowed" }
        val uri = parse(origin)
        val isOrigin =
            uri.scheme?.lowercase() in ALLOWED_SCHEMES &&
                !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null &&
                uri.rawPath.isNullOrEmpty() &&
                uri.rawQuery == null &&
                uri.rawFragment == null
        require(isOrigin) { "cors.allowed-origins entry must look like https://host[:port]: '$origin'" }
    }

    private fun parse(origin: String): URI = try {
        URI(origin)
    } catch (exception: URISyntaxException) {
        throw IllegalArgumentException("cors.allowed-origins entry is not a valid URI: '$origin'", exception)
    }
}
