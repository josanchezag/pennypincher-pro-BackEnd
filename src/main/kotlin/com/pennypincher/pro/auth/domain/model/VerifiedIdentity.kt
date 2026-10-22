package com.pennypincher.pro.auth.domain.model

import com.pennypincher.pro.user.domain.model.IdentityProfile

/** An identity whose ID token was fully verified: signature, issuer, audience, expiry and e-mail. */
data class VerifiedIdentity(val subject: String, val profile: IdentityProfile)
