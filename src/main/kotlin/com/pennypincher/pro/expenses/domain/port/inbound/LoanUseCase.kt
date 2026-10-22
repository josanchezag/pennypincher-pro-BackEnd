package com.pennypincher.pro.expenses.domain.port.inbound

import com.pennypincher.pro.expenses.domain.model.Loan
import com.pennypincher.pro.expenses.domain.model.LoanCommand

/** Managing the caller's loans; the summary adds up monthly installments and outstanding balances. */
interface LoanUseCase : BalanceAccountUseCase<Loan, LoanCommand>
