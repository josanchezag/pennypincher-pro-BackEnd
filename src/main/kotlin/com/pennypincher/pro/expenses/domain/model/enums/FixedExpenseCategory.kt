package com.pennypincher.pro.expenses.domain.model.enums

/**
 * Kind of fixed monthly expense. The declaration order is the order in which fixed expense lists and
 * summaries are grouped. CHARITY feeds the charity component of the monthly budget.
 */
enum class FixedExpenseCategory {
    FOOD,
    EDUCATION,
    TRANSPORT,
    UTILITIES,
    INSURANCE,
    HOUSING,
    HEALTH,
    CHARITY,
    COMMUNICATION,
    TAXES,
    OTHER,
}
