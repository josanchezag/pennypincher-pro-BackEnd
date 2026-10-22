package com.pennypincher.pro.shared.infrastructure.security.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher

/**
 * Ignores the `Authorization` header on public paths, so a stale access token sent along with
 * `/auth/refresh` (or any other public call) cannot turn it into a 401.
 */
class PublicPathBearerTokenResolver(publicPaths: List<String>) : BearerTokenResolver {
    private val delegate = DefaultBearerTokenResolver()
    private val publicRequests = OrRequestMatcher(publicPaths.map(AntPathRequestMatcher::antMatcher))

    override fun resolve(request: HttpServletRequest): String? =
        if (publicRequests.matches(request)) null else delegate.resolve(request)
}
