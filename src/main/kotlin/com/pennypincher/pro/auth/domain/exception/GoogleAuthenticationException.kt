package com.pennypincher.pro.auth.domain.exception

import com.pennypincher.pro.shared.domain.exception.BusinessException
import com.pennypincher.pro.shared.domain.exception.ErrorCode

/** Google rejected the authorization code, or the ID token it returned is not acceptable. */
class GoogleAuthenticationException(message: String, cause: Throwable? = null) :
    BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, message, cause)
