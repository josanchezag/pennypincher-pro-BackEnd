package com.pennypincher.pro.shared.domain.exception

/** Stable, machine-readable error codes returned in the `error.code` field of every error response. */
enum class ErrorCode {
    VALIDATION_FAILED,
    UNAUTHORIZED,
    FORBIDDEN,
    FORBIDDEN_ORIGIN,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    CONFLICT,
    GOOGLE_AUTH_FAILED,
    IDENTITY_PROVIDER_UNAVAILABLE,
    INTERNAL_ERROR,
}
