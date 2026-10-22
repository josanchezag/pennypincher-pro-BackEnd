package com.pennypincher.pro.shared.infrastructure.security.config

import com.pennypincher.pro.shared.infrastructure.security.jwt.UserPrincipalJwtConverter
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Default-deny, stateless filter chain. Only the auth endpoints and the health probes are public,
 * plus the API docs when springdoc is enabled (local profile only). Every other request needs a
 * valid RS256 access token (`Authorization: Bearer`), validated by the resource server against the
 * configured public key. CORS rules come from the `CorsConfigurationSource` bean.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${springdoc.swagger-ui.enabled:false}") private val apiDocsPublic: Boolean,
    private val errorHandler: JsonSecurityErrorHandler,
) {
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
            exceptionHandling {
                authenticationEntryPoint = errorHandler
                accessDeniedHandler = errorHandler
            }
            oauth2ResourceServer {
                bearerTokenResolver = PublicPathBearerTokenResolver(publicPaths)
                authenticationEntryPoint = errorHandler
                accessDeniedHandler = errorHandler
                jwt { jwtAuthenticationConverter = UserPrincipalJwtConverter() }
            }
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
