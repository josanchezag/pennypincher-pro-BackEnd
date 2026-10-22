package com.pennypincher.pro.expenses.domain.port.inbound

import com.pennypincher.pro.expenses.domain.model.BalanceAccount
import com.pennypincher.pro.expenses.domain.model.BalanceAccountCommand
import com.pennypincher.pro.expenses.domain.model.BalanceTotals
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase
import com.pennypincher.pro.shared.domain.port.inbound.SummaryUseCase

/** Managing the caller's [BalanceAccount]s of one kind: CRUD without category, plus their totals. */
interface BalanceAccountUseCase<T : BalanceAccount, Cmd : BalanceAccountCommand> :
    CrudUseCase<T, Cmd, Nothing>,
    SummaryUseCase<BalanceTotals>
