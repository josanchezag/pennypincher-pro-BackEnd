package com.pennypincher.pro.shared.infrastructure.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

/**
 * CSRF protection for the endpoints authenticated only by the refresh cookie: the request must carry
 * an `Origin` header from the CORS allowlist. Needed because the cookie may use `SameSite=None` when
 * frontend and API live on different sites. Rejections are rendered by [rejectionHandler].
 */
class CookieEndpointOriginGuard(
    allowedOrigins: Collection<String>,
    private val guardedRequests: RequestMatcher,
    private val rejectionHandler: (HttpServletRequest, HttpServletResponse, Exception) -> Unit,
) : OncePerRequestFilter() {
    private val allowedOrigins = allowedOrigins.map(String::lowercase).toSet()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !guardedRequests.matches(request)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val origin = request.getHeader(HttpHeaders.ORIGIN)?.trim()?.lowercase()
        if (origin != null && origin in allowedOrigins) {
            chain.doFilter(request, response)
        } else {
            rejectionHandler(request, response, ForbiddenOriginException())
        }
    }

    companion object {
        fun guarding(vararg endpoints: Pair<HttpMethod, String>): RequestMatcher =
            OrRequestMatcher(endpoints.map { (method, path) -> AntPathRequestMatcher.antMatcher(method, path) })
    }
}
