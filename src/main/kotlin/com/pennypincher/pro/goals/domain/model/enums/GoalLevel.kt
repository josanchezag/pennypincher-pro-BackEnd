package com.pennypincher.pro.goals.domain.model.enums

/**
 * Financial independence level a goal belongs to, with the text shown to the user. The declaration order is
 * the fixed display order of goals and levels. A level classifies goals; several goals can share one.
 */
enum class GoalLevel(val description: String) {
    FINANCIAL_SECURITY("Expenses covered for 6 months without income"),
    FINANCIAL_INDEPENDENCE("Passive income covers all basic expenses"),
    FINANCIAL_FREEDOM("Passive income covers desired lifestyle"),
    ABSOLUTE_FREEDOM("Unlimited financial choices"),
}
