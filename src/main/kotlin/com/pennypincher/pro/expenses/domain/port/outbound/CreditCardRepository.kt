package com.pennypincher.pro.expenses.domain.port.outbound

import com.pennypincher.pro.expenses.domain.model.CreditCard

/** Persistence port for credit cards. */
interface CreditCardRepository : BalanceAccountRepository<CreditCard>
