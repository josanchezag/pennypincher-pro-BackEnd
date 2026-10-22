package com.pennypincher.pro.shared.infrastructure.web

/** Base paths shared by every REST controller. */
object ApiPaths {
    const val API_V1 = "/api/v1"
    const val AUTH = "$API_V1/auth"
    const val USERS = "$API_V1/users"
}
