package com.pennypincher.pro.shared.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Browser origins allowed to call the API, bound from `cors.allowed-origins`
 * (environment variable `CORS_ALLOWED_ORIGINS`, comma-separated). Without the variable the local
 * origins `http://localhost:3000` (Flutter) and `http://localhost:8080` (Swagger UI) apply; `prod`
 * requires it. The same list feeds the `Origin` check of the refresh and logout endpoints.
 */
@ConfigurationProperties(prefix = "cors")
data class CorsProperties(val allowedOrigins: List<String>)
