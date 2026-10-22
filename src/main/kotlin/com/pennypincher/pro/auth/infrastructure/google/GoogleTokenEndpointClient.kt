package com.pennypincher.pro.auth.infrastructure.google

import com.fasterxml.jackson.annotation.JsonProperty
import com.pennypincher.pro.auth.domain.exception.GoogleAuthenticationException
import com.pennypincher.pro.auth.domain.exception.IdentityProviderUnavailableException
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

/**
 * Calls Google's token endpoint to exchange an authorization code (with its PKCE verifier) for an ID
 * token. Only a `400` about the code or verifier (`invalid_grant`, `invalid_request`) is the user's
 * fault; any other error (bad client credentials, rate limiting, 5xx, I/O) is reported as an outage.
 */
class GoogleTokenEndpointClient(
    private val restClient: RestClient,
    private val tokenUri: String,
    private val clientId: String,
    private val clientSecret: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun exchange(authorization: GoogleAuthorizationCode): String {
        val response = try {
            restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formFor(authorization))
                .retrieve()
                .body(GoogleTokenResponse::class.java)
        } catch (exception: RestClientResponseException) {
            throw translate(exception)
        } catch (exception: RestClientException) {
            throw IdentityProviderUnavailableException("Google token endpoint is unreachable", exception)
        }
        return response?.idToken?.takeIf(String::isNotBlank)
            ?: throw GoogleAuthenticationException("Google did not return an ID token")
    }

    private fun translate(exception: RestClientResponseException): RuntimeException {
        val error = runCatching { exception.getResponseBodyAs(GoogleErrorResponse::class.java)?.error }.getOrNull()
        val status = exception.statusCode.value()
        if (status == HttpStatus.BAD_REQUEST.value() && error in USER_ERRORS) {
            return GoogleAuthenticationException("Google rejected the authorization code ($error)", exception)
        }
        if (error == INVALID_CLIENT) {
            logger.error(
                "Google rejected the OAuth client credentials (HTTP {}); check GOOGLE_CLIENT_ID/SECRET",
                status,
            )
        } else {
            logger.warn("Google token endpoint failed with HTTP {} ({})", status, error)
        }
        return IdentityProviderUnavailableException("Google token endpoint failed with HTTP $status", exception)
    }

    private fun formFor(authorization: GoogleAuthorizationCode) = LinkedMultiValueMap<String, String>().apply {
        add("grant_type", GRANT_TYPE)
        add("code", authorization.code)
        add("code_verifier", authorization.codeVerifier)
        add("redirect_uri", authorization.redirectUri)
        add("client_id", clientId)
        add("client_secret", clientSecret)
    }

    private data class GoogleTokenResponse(@JsonProperty("id_token") val idToken: String?)

    private data class GoogleErrorResponse(@JsonProperty("error") val error: String?)

    private companion object {
        const val GRANT_TYPE = "authorization_code"
        const val INVALID_CLIENT = "invalid_client"
        val USER_ERRORS = setOf("invalid_grant", "invalid_request")
    }
}
