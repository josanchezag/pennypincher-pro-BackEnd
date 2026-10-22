package com.pennypincher.pro.user.domain.port.inbound

import com.pennypincher.pro.user.domain.model.ProfileUpdate
import com.pennypincher.pro.user.domain.model.User
import java.util.UUID

/** Reading and editing the caller's own profile. `userId` always comes from the access token. */
interface UserProfileUseCase {
    fun getProfile(userId: UUID): User

    fun updateProfile(userId: UUID, update: ProfileUpdate): User
}
