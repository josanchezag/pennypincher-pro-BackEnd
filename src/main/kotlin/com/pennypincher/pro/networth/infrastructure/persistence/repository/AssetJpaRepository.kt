package com.pennypincher.pro.networth.infrastructure.persistence.repository

import com.pennypincher.pro.networth.infrastructure.persistence.entity.AssetEntity
import com.pennypincher.pro.shared.infrastructure.persistence.repository.CategorizedAmountJpaRepository

interface AssetJpaRepository : CategorizedAmountJpaRepository<AssetEntity>
