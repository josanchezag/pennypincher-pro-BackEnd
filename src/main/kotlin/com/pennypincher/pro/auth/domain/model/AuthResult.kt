package com.pennypincher.pro.auth.domain.model

import com.pennypincher.pro.user.domain.model.User

/** Outcome of every successful sign-in or refresh: a new token pair and the signed-in user. */
data class AuthResult(val accessToken: AccessToken, val refreshToken: String, val user: User)
