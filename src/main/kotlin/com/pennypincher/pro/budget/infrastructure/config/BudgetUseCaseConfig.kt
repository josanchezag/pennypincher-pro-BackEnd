package com.pennypincher.pro.budget.infrastructure.config

import com.pennypincher.pro.budget.application.usecase.IncomeSourceUseCaseImpl
import com.pennypincher.pro.budget.domain.port.inbound.IncomeSourceUseCase
import com.pennypincher.pro.budget.domain.port.outbound.IncomeSourceRepository
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Wires the framework-free budget use cases. */
@Configuration
class BudgetUseCaseConfig {
    @Bean
    fun incomeSourceUseCase(incomeSourceRepository: IncomeSourceRepository, clock: Clock): IncomeSourceUseCase =
        IncomeSourceUseCaseImpl(incomeSourceRepository, clock)
}
