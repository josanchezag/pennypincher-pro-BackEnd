package com.pennypincher.pro.user.infrastructure.web.controller

import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import com.pennypincher.pro.user.domain.model.ProfileUpdate
import com.pennypincher.pro.user.domain.port.inbound.UserProfileUseCase
import com.pennypincher.pro.user.infrastructure.web.dto.UpdateProfileRequest
import com.pennypincher.pro.user.infrastructure.web.dto.UserResponse
import com.pennypincher.pro.user.infrastructure.web.mapper.UserResponseMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.USERS)
class UserController(
    private val userProfileUseCase: UserProfileUseCase,
    private val envelopes: ResponseEnvelopeFactory,
) {
    @GetMapping(ME)
    fun getProfile(@AuthenticatedUser principal: UserPrincipal): ApiResponse<UserResponse> =
        envelopes.success(UserResponseMapper.toResponse(userProfileUseCase.getProfile(principal.userId)))

    @PatchMapping(ME)
    fun updateProfile(
        @AuthenticatedUser principal: UserPrincipal,
        @RequestBody request: UpdateProfileRequest,
    ): ApiResponse<UserResponse> {
        val updated = userProfileUseCase.updateProfile(principal.userId, ProfileUpdate(request.name))
        return envelopes.success(UserResponseMapper.toResponse(updated))
    }

    private companion object {
        const val ME = "/me"
    }
}
