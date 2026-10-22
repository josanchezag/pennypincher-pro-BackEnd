package com.pennypincher.pro.auth.domain.port.outbound

import com.pennypincher.pro.auth.domain.model.GoogleAuthorizationCode
import com.pennypincher.pro.auth.domain.model.VerifiedIdentity

/** Exchanges an authorization code with the identity provider and returns the verified identity. */
interface IdentityProviderPort {
    fun authenticate(authorization: GoogleAuthorizationCode): VerifiedIdentity
}
