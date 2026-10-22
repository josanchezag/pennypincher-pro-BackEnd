package com.pennypincher.pro.shared.infrastructure.security.filter

import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class CookieEndpointOriginGuardTest {
    private val rejections = mutableListOf<Exception>()
    private val guard =
        CookieEndpointOriginGuard(
            allowedOrigins = listOf("http://localhost:3000"),
            guardedRequests = CookieEndpointOriginGuard.guarding(HttpMethod.POST to "/api/v1/auth/refresh"),
            rejectionHandler = { _, _, exception -> rejections += exception },
        )

    @Test
    fun should_pass_request_when_origin_is_allowed() {
        val chain = mockk<FilterChain>(relaxed = true)

        guard.doFilter(refreshRequest("http://LOCALHOST:3000"), MockHttpServletResponse(), chain)

        verify(exactly = 1) { chain.doFilter(any(), any()) }
        assertTrue(rejections.isEmpty())
    }

    @Test
    fun should_reject_request_when_origin_is_missing_or_not_allowed() {
        val chain = mockk<FilterChain>(relaxed = true)

        listOf(null, "https://evil.example.com", "null").forEach { origin ->
            guard.doFilter(refreshRequest(origin), MockHttpServletResponse(), chain)
        }

        verify(exactly = 0) { chain.doFilter(any(), any()) }
        assertEquals(3, rejections.filterIsInstance<ForbiddenOriginException>().size)
    }

    @Test
    fun should_ignore_requests_to_other_endpoints_or_methods() {
        val chain = mockk<FilterChain>(relaxed = true)

        guard.doFilter(request("POST", "/api/v1/auth/google/callback"), MockHttpServletResponse(), chain)
        guard.doFilter(request("OPTIONS", "/api/v1/auth/refresh"), MockHttpServletResponse(), chain)

        verify(exactly = 2) { chain.doFilter(any(), any()) }
        assertTrue(rejections.isEmpty())
    }

    private fun refreshRequest(origin: String?) = request("POST", "/api/v1/auth/refresh").apply {
        origin?.let { addHeader("Origin", it) }
    }

    private fun request(method: String, path: String) = MockHttpServletRequest(method, path).apply {
        servletPath = path
    }
}
