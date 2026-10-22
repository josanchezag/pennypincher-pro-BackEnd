package com.pennypincher.pro.auth.domain.model

import java.time.Duration

/** A signed access token and how long it stays valid. */
data class AccessToken(val value: String, val expiresIn: Duration)
