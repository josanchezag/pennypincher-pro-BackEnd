package com.pennypincher.pro.auth.infrastructure.config

import com.pennypincher.pro.auth.infrastructure.google.GoogleIdTokenValidators
import com.pennypincher.pro.auth.infrastructure.google.GoogleIdTokenVerifier
import com.pennypincher.pro.auth.infrastructure.google.GoogleTokenClient
import com.pennypincher.pro.auth.infrastructure.google.GoogleTokenEndpointClient
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

/** Wires the Google adapter: HTTP client with timeouts, JWKS-backed ID token decoder and circuit breaker. */
@Configuration
class GoogleClientConfig(private val properties: GoogleProperties) {
    @Bean
    fun googleTokenClient(
        restClientBuilder: RestClient.Builder,
        circuitBreakers: CircuitBreakerRegistry,
    ): GoogleTokenClient {
        val restClient = restClientBuilder.requestFactory(requestFactory()).build()
        val endpointClient =
            GoogleTokenEndpointClient(restClient, properties.tokenUri, properties.clientId, properties.clientSecret)
        val decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .restOperations(RestTemplate(requestFactory()))
            .build()
            .apply { setJwtValidator(GoogleIdTokenValidators.create(properties.clientId)) }
        return GoogleTokenClient(
            endpointClient,
            GoogleIdTokenVerifier(decoder),
            circuitBreakers.circuitBreaker(CIRCUIT_BREAKER),
        )
    }

    private fun requestFactory() = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(properties.connectTimeout)
        setReadTimeout(properties.readTimeout)
    }

    companion object {
        const val CIRCUIT_BREAKER = "google-token"
    }
}
