package com.pennypincher.pro.auth.domain.exception

import com.pennypincher.pro.shared.domain.exception.BusinessException
import com.pennypincher.pro.shared.domain.exception.ErrorCode

/**
 * The refresh token was rotated moments ago by another request of the same browser (for example two
 * tabs refreshing at once). The request is rejected with `REFRESH_SUPERSEDED`, but the session and its
 * cookie are kept: the browser already holds the newer cookie, so the client can retry the refresh once.
 */
class ConcurrentRefreshException :
    BusinessException(ErrorCode.REFRESH_SUPERSEDED, "Refresh token was just rotated by a concurrent request")
