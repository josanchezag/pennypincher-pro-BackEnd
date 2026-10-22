package com.pennypincher.pro.shared.infrastructure.web.advice

import com.pennypincher.pro.shared.domain.exception.ErrorCode
import org.springframework.http.HttpStatus

/** HTTP status returned for each error code. */
object ErrorStatusMapping {
    private val STATUS_BY_CODE =
        mapOf(
            ErrorCode.VALIDATION_FAILED to HttpStatus.BAD_REQUEST,
            ErrorCode.UNAUTHORIZED to HttpStatus.UNAUTHORIZED,
            ErrorCode.REFRESH_SUPERSEDED to HttpStatus.UNAUTHORIZED,
            ErrorCode.GOOGLE_AUTH_FAILED to HttpStatus.UNAUTHORIZED,
            ErrorCode.FORBIDDEN to HttpStatus.FORBIDDEN,
            ErrorCode.FORBIDDEN_ORIGIN to HttpStatus.FORBIDDEN,
            ErrorCode.NOT_FOUND to HttpStatus.NOT_FOUND,
            ErrorCode.METHOD_NOT_ALLOWED to HttpStatus.METHOD_NOT_ALLOWED,
            ErrorCode.CONFLICT to HttpStatus.CONFLICT,
            ErrorCode.UNSUPPORTED_MEDIA_TYPE to HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE to HttpStatus.SERVICE_UNAVAILABLE,
            ErrorCode.INTERNAL_ERROR to HttpStatus.INTERNAL_SERVER_ERROR,
        )

    fun statusOf(code: ErrorCode): HttpStatus = STATUS_BY_CODE.getValue(code)
}
