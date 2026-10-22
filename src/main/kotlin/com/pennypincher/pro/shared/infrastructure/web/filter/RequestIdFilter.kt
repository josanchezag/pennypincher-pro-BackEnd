package com.pennypincher.pro.shared.infrastructure.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Gives every request an id: the incoming `X-Request-ID` when it is a valid UUID, otherwise a new
 * random one. The id is stored as a request attribute and echoed in the response header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val requestId = resolve(request.getHeader(HEADER))
        request.setAttribute(ATTRIBUTE, requestId)
        response.setHeader(HEADER, requestId)
        chain.doFilter(request, response)
    }

    companion object {
        const val HEADER = "X-Request-ID"
        const val ATTRIBUTE = "com.pennypincher.pro.requestId"

        fun resolve(candidate: String?): String = candidate?.trim()?.let(::parseUuid) ?: UUID.randomUUID().toString()

        private fun parseUuid(value: String): String? = runCatching { UUID.fromString(value) }
            .getOrNull()
            ?.takeIf { it.toString().equals(value, ignoreCase = true) }
            ?.toString()
    }
}
