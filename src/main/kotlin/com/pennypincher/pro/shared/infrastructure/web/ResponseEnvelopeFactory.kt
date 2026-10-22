package com.pennypincher.pro.shared.infrastructure.web

import com.pennypincher.pro.shared.domain.exception.ErrorCode
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiError
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiErrorDetail
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiErrorResponse
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.dto.ResponseMeta
import com.pennypincher.pro.shared.infrastructure.web.filter.RequestIdFilter
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

/** Builds the standard success and error envelopes with the current timestamp and request id. */
@Component
class ResponseEnvelopeFactory(private val clock: Clock) {
    fun <T> success(data: T): ApiResponse<T> = ApiResponse(data, meta())

    fun error(code: ErrorCode, message: String, details: List<ApiErrorDetail> = emptyList()): ApiErrorResponse =
        ApiErrorResponse(ApiError(code.name, message, details.ifEmpty { null }), meta())

    private fun meta(): ResponseMeta =
        ResponseMeta(Instant.now(clock).truncatedTo(ChronoUnit.MILLIS), currentRequestId())

    private fun currentRequestId(): String {
        val stored = RequestContextHolder.getRequestAttributes()
            ?.getAttribute(RequestIdFilter.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) as? String
        return stored ?: RequestIdFilter.resolve(null)
    }
}
