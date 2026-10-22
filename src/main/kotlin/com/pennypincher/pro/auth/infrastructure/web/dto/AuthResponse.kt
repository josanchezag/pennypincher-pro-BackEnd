package com.pennypincher.pro.auth.infrastructure.web.dto

import com.pennypincher.pro.user.infrastructure.web.dto.UserResponse

/** Body returned after sign-in and refresh. The refresh token travels only in the cookie. */
data class AuthResponse(val accessToken: String, val tokenType: String, val expiresIn: Long, val user: UserResponse)
