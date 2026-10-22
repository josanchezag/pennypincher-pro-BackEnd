package com.pennypincher.pro.auth.infrastructure.web

import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.infrastructure.web.dto.AuthResponse
import com.pennypincher.pro.auth.infrastructure.web.mapper.AuthWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

/** Builds the HTTP answer shared by every endpoint that issues tokens: envelope body plus refresh cookie. */
@Component
class AuthResponseWriter(private val cookies: RefreshCookieFactory, private val envelopes: ResponseEnvelopeFactory) {
    fun write(result: AuthResult): ResponseEntity<ApiResponse<AuthResponse>> = ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken).toString())
        .header(HttpHeaders.CACHE_CONTROL, NO_STORE)
        .body(envelopes.success(AuthWebMapper.toResponse(result)))

    private companion object {
        const val NO_STORE = "no-store"
    }
}
