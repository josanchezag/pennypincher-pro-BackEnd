package com.pennypincher.pro.auth.infrastructure.config

import com.pennypincher.pro.auth.application.usecase.AuthUseCaseImpl
import com.pennypincher.pro.auth.application.usecase.DevLoginUseCaseImpl
import com.pennypincher.pro.auth.application.usecase.RefreshTokenCodec
import com.pennypincher.pro.auth.application.usecase.TokenPairIssuer
import com.pennypincher.pro.auth.application.usecase.UserUpserter
import com.pennypincher.pro.auth.domain.port.inbound.AuthUseCase
import com.pennypincher.pro.auth.domain.port.inbound.DevLoginUseCase
import com.pennypincher.pro.auth.domain.port.outbound.AccessTokenIssuer
import com.pennypincher.pro.auth.domain.port.outbound.IdentityProviderPort
import com.pennypincher.pro.auth.domain.port.outbound.RefreshTokenRepository
import com.pennypincher.pro.shared.infrastructure.security.jwt.JwtProperties
import com.pennypincher.pro.user.domain.port.outbound.UserRepository
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Wires the framework-free auth use cases. */
@Configuration
class AuthUseCaseConfig {
    @Bean
    fun refreshTokenCodec(): RefreshTokenCodec = RefreshTokenCodec()

    @Bean
    fun tokenPairIssuer(
        accessTokenIssuer: AccessTokenIssuer,
        refreshTokenRepository: RefreshTokenRepository,
        codec: RefreshTokenCodec,
        jwtProperties: JwtProperties,
        clock: Clock,
    ): TokenPairIssuer =
        TokenPairIssuer(accessTokenIssuer, refreshTokenRepository, codec, jwtProperties.refreshTokenTtl, clock)

    @Bean
    fun userUpserter(userRepository: UserRepository, clock: Clock): UserUpserter = UserUpserter(userRepository, clock)

    @Bean
    fun authUseCase(
        identityProvider: IdentityProviderPort,
        userUpserter: UserUpserter,
        tokenPairIssuer: TokenPairIssuer,
        googleProperties: GoogleProperties,
    ): AuthUseCase = AuthUseCaseImpl(
        identityProvider,
        userUpserter,
        tokenPairIssuer,
        googleProperties.allowedRedirectUris.map(String::trim).filter(String::isNotEmpty).toSet(),
    )

    @Bean
    @Profile(DEV_LOGIN_PROFILE)
    fun devLoginUseCase(userUpserter: UserUpserter, tokenPairIssuer: TokenPairIssuer): DevLoginUseCase =
        DevLoginUseCaseImpl(userUpserter, tokenPairIssuer)

    companion object {
        /** Dev-login exists only with the `local` profile, and never when `prod` is active too. */
        const val DEV_LOGIN_PROFILE = "local & !prod"
    }
}
