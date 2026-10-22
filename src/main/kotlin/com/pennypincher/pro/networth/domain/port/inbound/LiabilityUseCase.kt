package com.pennypincher.pro.networth.domain.port.inbound

import com.pennypincher.pro.networth.domain.model.Liability
import com.pennypincher.pro.networth.domain.model.LiabilityCommand
import com.pennypincher.pro.networth.domain.model.enums.LiabilityCategory
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase

/** Managing the caller's liabilities; [total] is the sum of the active current balances. */
interface LiabilityUseCase : CrudUseCase<Liability, LiabilityCommand, LiabilityCategory>
