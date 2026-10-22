package com.pennypincher.pro.networth.domain.port.inbound

import com.pennypincher.pro.networth.domain.model.Asset
import com.pennypincher.pro.networth.domain.model.AssetCommand
import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase

/** Managing the caller's assets; [total] is the sum of the active asset values. */
interface AssetUseCase : CrudUseCase<Asset, AssetCommand, AssetCategory>
