package com.pennypincher.pro.auth.infrastructure.web.mapper

import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode
import com.pennypincher.pro.auth.infrastructure.web.dto.AuthResponse
import com.pennypincher.pro.auth.infrastructure.web.dto.GoogleCallbackRequest
import com.pennypincher.pro.user.infrastructure.web.mapper.UserResponseMapper

/** Converts auth HTTP DTOs to domain values and back. */
object AuthWebMapper {
    const val TOKEN_TYPE = "Bearer"

    /** Call only after bean validation, which guarantees every field is present. */
    fun toAuthorizationCode(request: GoogleCallbackRequest): GoogleAuthorizationCode = GoogleAuthorizationCode(
        code = requireNotNull(request.code),
        redirectUri = requireNotNull(request.redirectUri),
        codeVerifier = requireNotNull(request.codeVerifier),
    )

    fun toResponse(result: AuthResult): AuthResponse = AuthResponse(
        accessToken = result.accessToken.value,
        tokenType = TOKEN_TYPE,
        expiresIn = result.accessToken.expiresIn.toSeconds(),
        user = UserResponseMapper.toResponse(result.user),
    )
}
