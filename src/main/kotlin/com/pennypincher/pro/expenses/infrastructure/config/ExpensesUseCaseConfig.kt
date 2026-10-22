package com.pennypincher.pro.expenses.infrastructure.config

import com.pennypincher.pro.expenses.application.usecase.CreditCardUseCaseImpl
import com.pennypincher.pro.expenses.application.usecase.ExpenseUseCaseImpl
import com.pennypincher.pro.expenses.application.usecase.LoanUseCaseImpl
import com.pennypincher.pro.expenses.application.usecase.SavingsAccountUseCaseImpl
import com.pennypincher.pro.expenses.domain.model.enums.FixedExpenseCategory
import com.pennypincher.pro.expenses.domain.model.enums.VariableExpenseCategory
import com.pennypincher.pro.expenses.domain.port.inbound.CreditCardUseCase
import com.pennypincher.pro.expenses.domain.port.inbound.ExpenseUseCase
import com.pennypincher.pro.expenses.domain.port.inbound.LoanUseCase
import com.pennypincher.pro.expenses.domain.port.inbound.SavingsAccountUseCase
import com.pennypincher.pro.expenses.domain.port.outbound.CreditCardRepository
import com.pennypincher.pro.expenses.domain.port.outbound.ExpenseRepository
import com.pennypincher.pro.expenses.domain.port.outbound.LoanRepository
import com.pennypincher.pro.expenses.domain.port.outbound.SavingsAccountRepository
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Wires the framework-free expense use cases. */
@Configuration
class ExpensesUseCaseConfig {
    @Bean
    fun creditCardUseCase(creditCardRepository: CreditCardRepository, clock: Clock): CreditCardUseCase =
        CreditCardUseCaseImpl(creditCardRepository, clock)

    @Bean
    fun loanUseCase(loanRepository: LoanRepository, clock: Clock): LoanUseCase = LoanUseCaseImpl(loanRepository, clock)

    @Bean
    fun fixedExpenseUseCase(
        fixedExpenseRepository: ExpenseRepository<FixedExpenseCategory>,
        clock: Clock,
    ): ExpenseUseCase<FixedExpenseCategory> = ExpenseUseCaseImpl(fixedExpenseRepository, clock, "Fixed expense")

    @Bean
    fun variableExpenseUseCase(
        variableExpenseRepository: ExpenseRepository<VariableExpenseCategory>,
        clock: Clock,
    ): ExpenseUseCase<VariableExpenseCategory> =
        ExpenseUseCaseImpl(variableExpenseRepository, clock, "Variable expense")

    @Bean
    fun savingsAccountUseCase(savingsAccountRepository: SavingsAccountRepository, clock: Clock): SavingsAccountUseCase =
        SavingsAccountUseCaseImpl(savingsAccountRepository, clock)
}
