package com.pennypincher.pro.networth.domain.port.outbound

import com.pennypincher.pro.networth.domain.model.Liability
import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import com.pennypincher.pro.shared.domain.port.outbound.SummableCrudRepository

/** Persistence port for liabilities; [sumActive] adds up the current balances. */
interface LiabilityRepository : SummableCrudRepository<Liability, LiabilityCategory>
