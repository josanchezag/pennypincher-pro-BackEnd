package com.pennypincher.pro.shared.infrastructure.config

import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUserArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Registers the `@AuthenticatedUser` argument resolver. */
@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(AuthenticatedUserArgumentResolver())
    }
}
