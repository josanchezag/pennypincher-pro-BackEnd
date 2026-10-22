package com.pennypincher.pro.expenses.domain.port.inbound

import com.pennypincher.pro.expenses.domain.model.SavingsAccount
import com.pennypincher.pro.expenses.domain.model.SavingsAccountCommand

/** Managing the caller's savings accounts; the summary adds up monthly contributions and current balances. */
interface SavingsAccountUseCase : BalanceAccountUseCase<SavingsAccount, SavingsAccountCommand>
