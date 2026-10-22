package com.pennypincher.pro.auth.application.usecase

import com.pennypincher.pro.auth.domain.model.AuthResult
import com.pennypincher.pro.auth.domain.model.VerifiedIdentity
import com.pennypincher.pro.auth.domain.port.inbound.DevLoginUseCase
import com.pennypincher.pro.user.domain.model.IdentityProfile

/** Signs in the fixed local test user. Only wired with the `local` profile. */
class DevLoginUseCaseImpl(private val userUpserter: UserUpserter, private val tokenPairIssuer: TokenPairIssuer) :
    DevLoginUseCase {
    override fun authenticateDevUser(): AuthResult = tokenPairIssuer.issueFor(userUpserter.upsert(DEV_IDENTITY))

    companion object {
        const val DEV_SUBJECT = "dev-local-user"
        const val DEV_EMAIL = "dev@pennypincher.local"
        const val DEV_NAME = "Dev User"
        val DEV_IDENTITY = VerifiedIdentity(DEV_SUBJECT, IdentityProfile(DEV_EMAIL, DEV_NAME, pictureUrl = null))
    }
}
