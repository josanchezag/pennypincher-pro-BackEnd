package com.pennypincher.pro.expenses.domain.port.outbound

import com.pennypincher.pro.expenses.domain.model.SavingsAccount

/** Persistence port for savings accounts. */
interface SavingsAccountRepository : BalanceAccountRepository<SavingsAccount>
