package com.pennypincher.pro.support

import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** Test-only protected endpoint that echoes the authenticated principal. */
@RestController
class PrincipalProbeController {
    @GetMapping(PATH)
    fun whoAmI(@AuthenticatedUser principal: UserPrincipal): UserPrincipal = principal

    companion object {
        const val PATH = "/api/v1/test-probe/principal"
    }
}
