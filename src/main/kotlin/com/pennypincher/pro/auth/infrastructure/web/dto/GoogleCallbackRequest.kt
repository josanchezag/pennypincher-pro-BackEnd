package com.pennypincher.pro.auth.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank

/** Body of `POST /auth/google/callback`. */
data class GoogleCallbackRequest(
    @field:NotBlank
    val code: String?,
    @field:NotBlank
    val redirectUri: String?,
    @field:NotBlank
    val codeVerifier: String?,
)
