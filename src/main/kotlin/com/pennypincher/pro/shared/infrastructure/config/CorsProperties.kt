package com.pennypincher.pro.shared.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Browser origins allowed to call the API, bound from `cors.allowed-origins`
 * (environment variable `CORS_ALLOWED_ORIGINS`, comma-separated).
 */
@ConfigurationProperties(prefix = "cors")
data class CorsProperties(val allowedOrigins: List<String>)
