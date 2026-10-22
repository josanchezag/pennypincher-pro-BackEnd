package com.pennypincher.pro.user.domain.exception

import com.pennypincher.pro.shared.domain.exception.BusinessException
import com.pennypincher.pro.shared.domain.exception.ErrorCode

/** The authenticated user has no row any more (for example, it was deleted after the token was issued). */
class UserNotFoundException : BusinessException(ErrorCode.NOT_FOUND, "User not found")
