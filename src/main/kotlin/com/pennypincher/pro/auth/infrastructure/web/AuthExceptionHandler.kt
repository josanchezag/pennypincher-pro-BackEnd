package com.pennypincher.pro.auth.infrastructure.web

import com.pennypincher.pro.auth.domain.exception.InvalidRefreshTokenException
import com.pennypincher.pro.auth.infrastructure.web.controller.AuthController
import com.pennypincher.pro.shared.domain.exception.ErrorCode
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiErrorResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** A rejected refresh token also clears the cookie, so the browser stops sending it. */
@RestControllerAdvice(assignableTypes = [AuthController::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthExceptionHandler(private val cookies: RefreshCookieFactory, private val envelopes: ResponseEnvelopeFactory) {
    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(exception: InvalidRefreshTokenException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
            .body(envelopes.error(ErrorCode.UNAUTHORIZED, exception.message.orEmpty()))
}
