package com.pennypincher.pro.auth.infrastructure.web.controller

import com.pennypincher.pro.auth.domain.port.inbound.AuthUseCase
import com.pennypincher.pro.auth.infrastructure.web.AuthResponseWriter
import com.pennypincher.pro.auth.infrastructure.web.dto.AuthResponse
import com.pennypincher.pro.auth.infrastructure.web.dto.GoogleCallbackRequest
import com.pennypincher.pro.auth.infrastructure.web.mapper.AuthWebMapper
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.AUTH)
class AuthController(private val authUseCase: AuthUseCase, private val responseWriter: AuthResponseWriter) {
    @PostMapping("/google/callback")
    fun googleCallback(@Valid @RequestBody request: GoogleCallbackRequest): ResponseEntity<ApiResponse<AuthResponse>> =
        responseWriter.write(authUseCase.authenticateWithGoogle(AuthWebMapper.toAuthorizationCode(request)))
}
