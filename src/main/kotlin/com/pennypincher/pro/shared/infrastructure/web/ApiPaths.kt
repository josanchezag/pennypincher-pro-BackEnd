package com.pennypincher.pro.shared.infrastructure.web

/** Base paths shared by every REST controller. */
object ApiPaths {
    const val API_V1 = "/api/v1"
    const val AUTH = "$API_V1/auth"
    const val REFRESH = "/refresh"
    const val LOGOUT = "/logout"
    const val AUTH_REFRESH = "$AUTH$REFRESH"
    const val AUTH_LOGOUT = "$AUTH$LOGOUT"
    const val USERS = "$API_V1/users"
    const val ASSETS = "$API_V1/assets"
    const val LIABILITIES = "$API_V1/liabilities"
    const val NET_WORTH = "$API_V1/net-worth"
}
