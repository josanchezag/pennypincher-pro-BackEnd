package com.pennypincher.pro.budget.infrastructure.config

import com.pennypincher.pro.budget.application.usecase.IncomeSourceUseCaseImpl
import com.pennypincher.pro.budget.application.usecase.MonthlyBudgetUseCaseImpl
import com.pennypincher.pro.budget.application.usecase.component.BudgetComponentStrategies
import com.pennypincher.pro.budget.domain.port.inbound.IncomeSourceUseCase
import com.pennypincher.pro.budget.domain.port.inbound.MonthlyBudgetUseCase
import com.pennypincher.pro.budget.domain.port.outbound.BudgetSourceTotals
import com.pennypincher.pro.budget.domain.port.outbound.IncomeSourceRepository
import com.pennypincher.pro.budget.domain.port.outbound.MonthlyBudgetReader
import com.pennypincher.pro.budget.domain.port.outbound.MonthlyBudgetWriter
import com.pennypincher.pro.budget.domain.service.BudgetCalculationService
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Wires the framework-free budget use cases. */
@Configuration
class BudgetUseCaseConfig {
    @Bean
    fun incomeSourceUseCase(incomeSourceRepository: IncomeSourceRepository, clock: Clock): IncomeSourceUseCase =
        IncomeSourceUseCaseImpl(incomeSourceRepository, clock)

    @Bean
    fun monthlyBudgetUseCase(
        budgetReader: MonthlyBudgetReader,
        budgetWriter: MonthlyBudgetWriter,
        sourceTotals: BudgetSourceTotals,
        clock: Clock,
    ): MonthlyBudgetUseCase = MonthlyBudgetUseCaseImpl(
        budgetReader = budgetReader,
        budgetWriter = budgetWriter,
        sourceTotals = sourceTotals,
        strategies = BudgetComponentStrategies.all(),
        calculator = BudgetCalculationService(),
        clock = clock,
    )
}
