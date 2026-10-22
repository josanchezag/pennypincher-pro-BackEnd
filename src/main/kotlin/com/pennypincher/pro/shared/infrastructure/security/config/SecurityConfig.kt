package com.pennypincher.pro.shared.infrastructure.security.config

import com.pennypincher.pro.shared.infrastructure.config.CorsOriginValidator
import com.pennypincher.pro.shared.infrastructure.config.CorsProperties
import com.pennypincher.pro.shared.infrastructure.security.filter.CookieEndpointOriginGuard
import com.pennypincher.pro.shared.infrastructure.security.jwt.UserPrincipalJwtConverter
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.CorsFilter

/**
 * Default-deny, stateless filter chain. Only the auth endpoints and the health probes are public,
 * plus the API docs when springdoc is enabled (local profile only). Every other request needs a
 * valid RS256 access token (`Authorization: Bearer`), validated by the resource server against the
 * configured public key. CORS rules come from the `CorsConfigurationSource` bean. The endpoints that
 * rely only on the refresh cookie additionally require an allowed `Origin` header (CSRF guard).
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${springdoc.swagger-ui.enabled:false}") private val apiDocsPublic: Boolean,
    private val errorHandler: JsonSecurityErrorHandler,
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val publicPaths = AUTH_PATHS + HEALTH_PATHS + if (apiDocsPublic) API_DOCS_PATHS else emptyList()
        http {
            csrf { disable() }
            addFilterBefore<CorsFilter>(cookieEndpointOriginGuard())
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

    private fun cookieEndpointOriginGuard() = CookieEndpointOriginGuard(
        allowedOrigins = CorsOriginValidator.validate(corsProperties.allowedOrigins),
        guardedRequests = CookieEndpointOriginGuard.guarding(
            HttpMethod.POST to ApiPaths.AUTH_REFRESH,
            HttpMethod.DELETE to ApiPaths.AUTH_LOGOUT,
        ),
        rejectionHandler = errorHandler::resolve,
    )

    companion object {
        val AUTH_PATHS = listOf("${ApiPaths.AUTH}/**")
        val HEALTH_PATHS = listOf("/actuator/health", "/actuator/health/**")
        val API_DOCS_PATHS = listOf("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
    }
}
