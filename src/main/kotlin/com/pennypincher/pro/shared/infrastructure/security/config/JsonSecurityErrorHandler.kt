package com.pennypincher.pro.shared.infrastructure.security.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Hands security failures raised in the filter chain to the MVC exception handlers, so a missing,
 * invalid or expired token gets the same JSON error envelope as any other error.
 */
@Component
class JsonSecurityErrorHandler(
    @Qualifier("handlerExceptionResolver") private val exceptionResolver: HandlerExceptionResolver,
) : AuthenticationEntryPoint,
    AccessDeniedHandler {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE)
        resolve(request, response, exception)
    }

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, exception: AccessDeniedException) =
        resolve(request, response, exception)

    /** Renders [exception] through the global exception handler. */
    fun resolve(request: HttpServletRequest, response: HttpServletResponse, exception: Exception) {
        exceptionResolver.resolveException(request, response, null, exception)
    }

    private companion object {
        const val BEARER_CHALLENGE = "Bearer"
    }
}
