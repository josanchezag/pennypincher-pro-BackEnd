package com.pennypincher.pro.shared.infrastructure.web.resolver

import com.pennypincher.pro.shared.domain.model.UserPrincipal
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/** Resolves `@AuthenticatedUser UserPrincipal` parameters from the request's authenticated principal. */
class AuthenticatedUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthenticatedUser::class.java) &&
            parameter.parameterType == UserPrincipal::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): UserPrincipal = (webRequest.userPrincipal as? Authentication)?.principal as? UserPrincipal
        ?: throw AuthenticationCredentialsNotFoundException("No authenticated user")
}
