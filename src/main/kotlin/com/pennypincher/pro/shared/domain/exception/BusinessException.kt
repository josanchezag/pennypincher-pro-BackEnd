package com.pennypincher.pro.shared.domain.exception

/** Root of every expected failure raised by the domain and application layers. */
abstract class BusinessException(val errorCode: ErrorCode, message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
