package com.pennypincher.pro.user.domain.port.outbound

import com.pennypincher.pro.user.domain.model.User
import java.util.UUID

/** Persistence port for users. Lookup by Google subject is the only query that is not scoped by user id. */
interface UserRepository {
    fun findById(id: UUID): User?

    fun findByGoogleSub(googleSub: String): User?

    fun save(user: User): User
}
