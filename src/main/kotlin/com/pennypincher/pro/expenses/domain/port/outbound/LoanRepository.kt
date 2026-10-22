package com.pennypincher.pro.expenses.domain.port.outbound

import com.pennypincher.pro.expenses.domain.model.Loan

/** Persistence port for loans. */
interface LoanRepository : BalanceAccountRepository<Loan>
