package com.pennypincher.pro.user.domain.model

/** Fields a user may change on their own profile. A null field is left unchanged. */
data class ProfileUpdate(val name: String?)
