package com.pennypincher.pro.auth.domain.port.outbound

import com.pennypincher.pro.auth.domain.model.AccessToken
import com.pennypincher.pro.user.domain.model.User

/** Signs short-lived access tokens for a user. */
interface AccessTokenIssuer {
    fun issue(user: User): AccessToken
}
