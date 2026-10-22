package com.pennypincher.pro.expenses.application.usecase

/** Field names of the amounts that credit cards and loans share, as reported in validation errors. */
internal object DebtFields {
    const val MONTHLY_PAYMENT = "monthlyPayment"
    const val TOTAL_BALANCE = "totalBalance"
}
