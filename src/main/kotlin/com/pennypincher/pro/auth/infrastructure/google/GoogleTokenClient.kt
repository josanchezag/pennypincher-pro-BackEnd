package com.pennypincher.pro.auth.infrastructure.google

import com.pennypincher.pro.auth.domain.exception.IdentityProviderUnavailableException
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode
import com.pennypincher.pro.auth.domain.model.VerifiedIdentity
import com.pennypincher.pro.auth.domain.port.outbound.IdentityProviderPort
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker

/**
 * Adapter from Google to [IdentityProviderPort]: exchanges the code and verifies the returned ID token
 * (which may fetch Google's JWKS) inside one circuit breaker. Rejected codes or tokens do not count as
 * failures; outages do. While the circuit is open, sign-in fails fast with 503.
 */
class GoogleTokenClient(
    private val endpointClient: GoogleTokenEndpointClient,
    private val idTokenVerifier: GoogleIdTokenVerifier,
    private val circuitBreaker: CircuitBreaker,
) : IdentityProviderPort {
    override fun authenticate(authorization: GoogleAuthorizationCode): VerifiedIdentity = try {
        circuitBreaker.executeSupplier { idTokenVerifier.verify(endpointClient.exchange(authorization)) }
    } catch (exception: CallNotPermittedException) {
        throw IdentityProviderUnavailableException("Google sign-in is temporarily unavailable", exception)
    }
}
