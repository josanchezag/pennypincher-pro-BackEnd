package com.pennypincher.pro.shared.infrastructure.web.controller

import com.pennypincher.pro.shared.domain.model.OwnedResource
import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import org.springframework.web.bind.annotation.GetMapping

/** CRUD endpoints of records without category: the list takes no filter. */
abstract class UncategorizedCrudController<T : OwnedResource, Cmd, Req, Res>(
    useCase: CrudUseCase<T, Cmd, Nothing>,
    envelopes: ResponseEnvelopeFactory,
) : AbstractCrudController<T, Cmd, Nothing, Req, Res>(useCase, envelopes) {
    @GetMapping
    fun list(@AuthenticatedUser principal: UserPrincipal): ApiResponse<List<Res>> = listResponse(principal, null)
}
