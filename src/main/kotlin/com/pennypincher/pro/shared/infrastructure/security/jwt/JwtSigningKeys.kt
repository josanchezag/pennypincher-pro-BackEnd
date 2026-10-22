package com.pennypincher.pro.shared.infrastructure.security.jwt

import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

/** The RSA key pair that signs (private) and verifies (public) access tokens. */
data class JwtSigningKeys(val publicKey: RSAPublicKey, val privateKey: RSAPrivateKey)
