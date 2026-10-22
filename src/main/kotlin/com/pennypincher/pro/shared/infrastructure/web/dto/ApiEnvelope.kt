package com.pennypincher.pro.shared.infrastructure.web.dto

import java.time.Instant

/** Metadata attached to every response: when it was produced and which request it answers. */
data class ResponseMeta(val timestamp: Instant, val requestId: String)

/** Successful response envelope: `{ data, meta }`. */
data class ApiResponse<T>(val data: T, val meta: ResponseMeta)

/** One field-level problem inside an error response. */
data class ApiErrorDetail(val field: String, val message: String)

/** The `error` object of an error response; `details` is omitted when empty. */
data class ApiError(val code: String, val message: String, val details: List<ApiErrorDetail>? = null)

/** Error response envelope: `{ error, meta }`. */
data class ApiErrorResponse(val error: ApiError, val meta: ResponseMeta)
