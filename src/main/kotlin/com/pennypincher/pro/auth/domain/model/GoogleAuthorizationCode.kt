package com.pennypincher.pro.auth.domain.model

/** What the browser obtained from Google with PKCE and hands to the backend for the code exchange. */
data class GoogleAuthorizationCode(val code: String, val redirectUri: String, val codeVerifier: String)
