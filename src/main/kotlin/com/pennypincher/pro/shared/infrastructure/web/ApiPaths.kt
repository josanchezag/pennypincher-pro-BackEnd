package com.pennypincher.pro.shared.infrastructure.web

/** Base paths shared by every REST controller. */
object ApiPaths {
    const val API_V1 = "/api/v1"
    const val AUTH = "$API_V1/auth"
    const val REFRESH = "/refresh"
    const val LOGOUT = "/logout"
    const val AUTH_REFRESH = "$AUTH$REFRESH"
    const val AUTH_LOGOUT = "$AUTH$LOGOUT"
    const val USERS = "$API_V1/users"
    const val ASSETS = "$API_V1/assets"
    const val LIABILITIES = "$API_V1/liabilities"
    const val NET_WORTH = "$API_V1/net-worth"
    const val INCOME_SOURCES = "$API_V1/income-sources"
    const val MONTHLY_BUDGETS = "$API_V1/monthly-budgets"
    const val CREDIT_CARDS = "$API_V1/credit-cards"
    const val LOANS = "$API_V1/loans"
    const val FIXED_EXPENSES = "$API_V1/fixed-expenses"
    const val VARIABLE_EXPENSES = "$API_V1/variable-expenses"
    const val SAVINGS_ACCOUNTS = "$API_V1/savings-accounts"
}
