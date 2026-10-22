package com.pennypincher.pro.shared.domain.model

/** Name rules shared by the per-user records (assets, income sources, expenses, ...). */
object RecordName {
    /** Longest name the `VARCHAR(150)` name columns accept. */
    const val MAX_LENGTH = 150

    /** Field that reports a name violation. */
    const val FIELD = "name"
}
