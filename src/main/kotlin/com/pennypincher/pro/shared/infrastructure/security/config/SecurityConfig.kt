package com.pennypincher.pro.shared.infrastructure.security.config

import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint

/**
 * Default-deny filter chain: only the auth endpoints and the health probes are public, plus the API
 * docs when springdoc is enabled (local profile only). CORS rules come from the
 * `CorsConfigurationSource` bean.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(@Value("\${springdoc.swagger-ui.enabled:false}") private val apiDocsPublic: Boolean) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val publicPaths = AUTH_PATHS + HEALTH_PATHS + if (apiDocsPublic) API_DOCS_PATHS else emptyList()
        http {
            csrf { disable() }
            cors { }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            exceptionHandling { authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED) }
            authorizeHttpRequests {
                publicPaths.forEach { path -> authorize(path, permitAll) }
                authorize(anyRequest, authenticated)
            }
        }
        return http.build()
    }

    companion object {
        val AUTH_PATHS = listOf("${ApiPaths.AUTH}/**")
        val HEALTH_PATHS = listOf("/actuator/health", "/actuator/health/**")
        val API_DOCS_PATHS = listOf("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
    }
}
