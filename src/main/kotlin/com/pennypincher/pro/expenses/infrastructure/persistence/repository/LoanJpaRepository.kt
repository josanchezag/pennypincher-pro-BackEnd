package com.pennypincher.pro.expenses.infrastructure.persistence.repository

import com.pennypincher.pro.expenses.infrastructure.persistence.entity.LoanEntity

interface LoanJpaRepository : BalanceAccountJpaRepository<LoanEntity>
