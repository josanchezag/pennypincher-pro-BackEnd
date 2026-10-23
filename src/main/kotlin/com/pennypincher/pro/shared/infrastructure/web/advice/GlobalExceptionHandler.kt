package com.pennypincher.pro.shared.infrastructure.web.advice

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.exc.InputCoercionException
import com.fasterxml.jackson.databind.JsonMappingException
import com.pennypincher.pro.shared.domain.exception.BusinessException
import com.pennypincher.pro.shared.domain.exception.ErrorCode
import com.pennypincher.pro.shared.domain.exception.ValidationException
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiErrorDetail
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Turns every exception into the standard error envelope. Unknown failures become a generic 500
 * without internal details; the cause is only logged.
 */
@RestControllerAdvice
class GlobalExceptionHandler(private val envelopes: ResponseEnvelopeFactory) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(exception: ValidationException): ResponseEntity<ApiErrorResponse> = respond(
        ErrorCode.VALIDATION_FAILED,
        exception.message,
        exception.violations.map {
            ApiErrorDetail(it.field, it.message)
        },
    )

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(exception: BusinessException): ResponseEntity<ApiErrorResponse> {
        if (ErrorStatusMapping.statusOf(exception.errorCode).is5xxServerError) {
            val cause = exception.cause
            logger.warn("{}: {} (cause: {})", exception.errorCode, exception.message, cause?.javaClass?.simpleName)
        }
        return respond(exception.errorCode, exception.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidBody(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val details =
            exception.bindingResult.fieldErrors.map { ApiErrorDetail(it.field, it.defaultMessage ?: INVALID_VALUE) }
        return respond(ErrorCode.VALIDATION_FAILED, "Validation failed for ${details.size} field(s)", details)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(exception: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> {
        val field = fieldOf(exception.cause)
        val details = if (field.isEmpty()) emptyList() else listOf(ApiErrorDetail(field, INVALID_VALUE))
        return respond(ErrorCode.VALIDATION_FAILED, "Request body is missing or malformed", details)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(exception: MethodArgumentTypeMismatchException): ResponseEntity<ApiErrorResponse> = respond(
        ErrorCode.VALIDATION_FAILED,
        "Validation failed for 1 field(s)",
        listOf(ApiErrorDetail(exception.name, INVALID_VALUE)),
    )

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(exception: AuthenticationException): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.UNAUTHORIZED, "Authentication is required")

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(exception: AccessDeniedException): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.FORBIDDEN, "Access to this resource is forbidden")

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(exception: NoResourceFoundException): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.NOT_FOUND, "Resource not found")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethod(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.METHOD_NOT_ALLOWED, "HTTP method ${exception.method} is not supported here")

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaType(exception: HttpMediaTypeNotSupportedException): ResponseEntity<ApiErrorResponse> =
        respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported")

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflict(exception: DataIntegrityViolationException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Data integrity violation: {}", exception.mostSpecificCause.message)
        return respond(ErrorCode.CONFLICT, "The request conflicts with existing data")
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected error", exception)
        return respond(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred")
    }

    private fun respond(
        code: ErrorCode,
        message: String?,
        details: List<ApiErrorDetail> = emptyList(),
    ): ResponseEntity<ApiErrorResponse> = ResponseEntity
        .status(ErrorStatusMapping.statusOf(code))
        .body(envelopes.error(code, message ?: code.name, details))

    /** The JSON field a body error points at, or empty when the body is malformed as a whole. */
    private fun fieldOf(cause: Throwable?): String = when (cause) {
        is JsonMappingException -> fieldPath(cause.path)
        is InputCoercionException -> (cause.processor as? JsonParser)?.currentName().orEmpty()
        else -> ""
    }

    private fun fieldPath(path: List<JsonMappingException.Reference>): String =
        path.joinToString(PATH_SEPARATOR) { it.fieldName ?: "[${it.index}]" }

    private companion object {
        const val INVALID_VALUE = "is invalid"
        const val PATH_SEPARATOR = "."
    }
}
