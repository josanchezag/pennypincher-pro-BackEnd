package com.pennypincher.pro.user.infrastructure.web.dto

/**
 * Body of `PATCH /users/me`. Only `name` can be changed; any other field (such as `email` or
 * `pictureUrl`) is ignored because it is not bound.
 */
data class UpdateProfileRequest(val name: String? = null)
