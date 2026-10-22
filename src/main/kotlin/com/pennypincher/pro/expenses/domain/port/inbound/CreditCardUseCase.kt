package com.pennypincher.pro.expenses.domain.port.inbound

import com.pennypincher.pro.expenses.domain.model.CreditCard
import com.pennypincher.pro.expenses.domain.model.CreditCardCommand

/** Managing the caller's credit cards; the summary adds up monthly payments and outstanding balances. */
interface CreditCardUseCase : BalanceAccountUseCase<CreditCard, CreditCardCommand>
