package com.pennypincher.pro.auth.infrastructure.web.controller

import com.pennypincher.pro.auth.domain.port.inbound.DevLoginUseCase
import com.pennypincher.pro.auth.infrastructure.config.AuthUseCaseConfig
import com.pennypincher.pro.auth.infrastructure.web.AuthResponseWriter
import com.pennypincher.pro.auth.infrastructure.web.dto.AuthResponse
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Local-only sign-in for a fixed test user. The bean does not exist outside the `local` profile. */
@RestController
@Profile(AuthUseCaseConfig.DEV_LOGIN_PROFILE)
@RequestMapping(ApiPaths.AUTH)
class DevLoginController(
    private val devLoginUseCase: DevLoginUseCase,
    private val responseWriter: AuthResponseWriter,
) {
    @PostMapping("/dev-login")
    fun devLogin(): ResponseEntity<ApiResponse<AuthResponse>> =
        responseWriter.write(devLoginUseCase.authenticateDevUser())
}
