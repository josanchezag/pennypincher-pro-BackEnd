package com.pennypincher.pro.auth.domain.exception

import com.pennypincher.pro.shared.domain.exception.BusinessException
import com.pennypincher.pro.shared.domain.exception.ErrorCode

/** The refresh token is missing, unknown, expired or revoked. */
class InvalidRefreshTokenException : BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is invalid or expired")
