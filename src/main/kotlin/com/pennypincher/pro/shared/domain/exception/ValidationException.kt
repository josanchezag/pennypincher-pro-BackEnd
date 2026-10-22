package com.pennypincher.pro.shared.domain.exception

/** One rejected input value: the field name as the client sent it and a readable reason. */
data class FieldViolation(val field: String, val message: String)

/** Input that breaks a business rule; carries every violated field. */
class ValidationException(val violations: List<FieldViolation>) :
    BusinessException(ErrorCode.VALIDATION_FAILED, "Validation failed for ${violations.size} field(s)") {
    constructor(field: String, message: String) : this(listOf(FieldViolation(field, message)))
}
