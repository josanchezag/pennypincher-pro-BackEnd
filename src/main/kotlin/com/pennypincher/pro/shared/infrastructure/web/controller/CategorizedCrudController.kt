package com.pennypincher.pro.shared.infrastructure.web.controller

import com.pennypincher.pro.shared.domain.model.OwnedResource
import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

/** CRUD endpoints of records with a category [C]: the list accepts an optional `?category=` filter. */
abstract class CategorizedCrudController<T : OwnedResource, Cmd, C, Req, Res>(
    useCase: CrudUseCase<T, Cmd, C>,
    envelopes: ResponseEnvelopeFactory,
) : AbstractCrudController<T, Cmd, C, Req, Res>(useCase, envelopes) {
    @GetMapping
    fun list(
        @AuthenticatedUser principal: UserPrincipal,
        @RequestParam(CATEGORY_PARAM, required = false) category: C?,
    ): ApiResponse<List<Res>> = listResponse(principal, category)

    private companion object {
        const val CATEGORY_PARAM = "category"
    }
}
