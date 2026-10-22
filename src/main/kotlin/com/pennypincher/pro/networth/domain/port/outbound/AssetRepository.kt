package com.pennypincher.pro.networth.domain.port.outbound

import com.pennypincher.pro.networth.domain.model.Asset
import com.pennypincher.pro.networth.domain.model.enums.AssetCategory
import com.pennypincher.pro.shared.domain.port.outbound.SummableCrudRepository

/** Persistence port for assets; [sumActive] adds up the asset values. */
interface AssetRepository : SummableCrudRepository<Asset, AssetCategory>
