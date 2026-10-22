package com.pennypincher.pro.shared.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {
    @Bean
    fun corsConfigurationSource(properties: CorsProperties): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration(API_PATH_PATTERN, buildCorsConfiguration(properties.allowedOrigins))
        return source
    }

    internal fun buildCorsConfiguration(origins: List<String>): CorsConfiguration = CorsConfiguration().apply {
        allowedOrigins = CorsOriginValidator.validate(origins)
        allowedMethods = ALLOWED_METHODS
        allowedHeaders = ALLOWED_HEADERS
        exposedHeaders = EXPOSED_HEADERS
        allowCredentials = true
        maxAge = PREFLIGHT_MAX_AGE_SECONDS
    }

    companion object {
        const val API_PATH_PATTERN = "/api/**"
        private const val PREFLIGHT_MAX_AGE_SECONDS = 3600L
        private const val REQUEST_ID_HEADER = "X-Request-ID"
        private val ALLOWED_METHODS = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        private val ALLOWED_HEADERS = listOf("Authorization", "Content-Type", REQUEST_ID_HEADER)
        private val EXPOSED_HEADERS = listOf("Location", REQUEST_ID_HEADER)
    }
}
