package com.pennypincher.pro.shared.domain.model

import java.util.UUID

/** A record that belongs to exactly one user. */
interface OwnedResource {
    val id: UUID
    val userId: UUID
}
