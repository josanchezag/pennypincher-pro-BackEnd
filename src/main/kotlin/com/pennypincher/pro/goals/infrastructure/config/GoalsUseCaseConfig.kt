package com.pennypincher.pro.goals.infrastructure.config

import com.pennypincher.pro.goals.application.usecase.FinancialGoalUseCaseImpl
import com.pennypincher.pro.goals.domain.port.inbound.FinancialGoalUseCase
import com.pennypincher.pro.goals.domain.port.outbound.FinancialGoalRepository
import com.pennypincher.pro.goals.domain.service.GoalProjectionService
import com.pennypincher.pro.networth.domain.port.inbound.NetWorthUseCase
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Wires the framework-free financial goal use cases. */
@Configuration
class GoalsUseCaseConfig {
    @Bean
    fun financialGoalUseCase(
        financialGoalRepository: FinancialGoalRepository,
        netWorthUseCase: NetWorthUseCase,
        clock: Clock,
    ): FinancialGoalUseCase =
        FinancialGoalUseCaseImpl(financialGoalRepository, netWorthUseCase, GoalProjectionService(), clock)
}
