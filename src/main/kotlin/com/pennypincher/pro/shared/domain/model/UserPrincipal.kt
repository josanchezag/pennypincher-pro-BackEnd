package com.pennypincher.pro.shared.domain.model

import java.util.UUID

/** The authenticated caller, taken from a validated access token. */
data class UserPrincipal(val userId: UUID, val email: String)
