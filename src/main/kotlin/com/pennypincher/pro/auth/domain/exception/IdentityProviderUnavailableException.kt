package com.pennypincher.pro.auth.domain.exception

import com.pennypincher.pro.shared.domain.exception.BusinessException
import com.pennypincher.pro.shared.domain.exception.ErrorCode

/** Google could not be reached, failed, or the circuit breaker in front of it is open. */
class IdentityProviderUnavailableException(message: String, cause: Throwable? = null) :
    BusinessException(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE, message, cause)
