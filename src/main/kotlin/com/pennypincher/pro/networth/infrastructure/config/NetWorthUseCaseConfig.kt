package com.pennypincher.pro.networth.infrastructure.config

import com.pennypincher.pro.networth.application.usecase.AssetUseCaseImpl
import com.pennypincher.pro.networth.application.usecase.LiabilityUseCaseImpl
import com.pennypincher.pro.networth.domain.port.inbound.AssetUseCase
import com.pennypincher.pro.networth.domain.port.inbound.LiabilityUseCase
import com.pennypincher.pro.networth.domain.port.outbound.AssetRepository
import com.pennypincher.pro.networth.domain.port.outbound.LiabilityRepository
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Wires the framework-free net worth use cases. */
@Configuration
class NetWorthUseCaseConfig {
    @Bean
    fun assetUseCase(assetRepository: AssetRepository, clock: Clock): AssetUseCase =
        AssetUseCaseImpl(assetRepository, clock)

    @Bean
    fun liabilityUseCase(liabilityRepository: LiabilityRepository, clock: Clock): LiabilityUseCase =
        LiabilityUseCaseImpl(liabilityRepository, clock)
}
