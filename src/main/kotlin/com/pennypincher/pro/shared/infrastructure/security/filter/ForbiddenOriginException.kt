package com.pennypincher.pro.shared.infrastructure.security.filter

import com.pennypincher.pro.shared.domain.exception.BusinessException
import com.pennypincher.pro.shared.domain.exception.ErrorCode

/** A cookie-authenticated request came without an allowed `Origin` header. */
class ForbiddenOriginException :
    BusinessException(ErrorCode.FORBIDDEN_ORIGIN, "Request origin is not allowed for this endpoint")
