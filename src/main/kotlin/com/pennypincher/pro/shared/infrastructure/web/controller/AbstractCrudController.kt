package com.pennypincher.pro.shared.infrastructure.web.controller

import com.pennypincher.pro.shared.domain.model.OwnedResource
import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.domain.port.inbound.CrudUseCase
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import jakarta.validation.Valid
import java.math.BigDecimal
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * REST endpoints shared by the per-user CRUD records: list (with an optional `category` filter),
 * total, get by id, create, update and soft delete. Subclasses add `@RestController` and `@RequestMapping` and
 * map between the HTTP DTOs and the domain.
 */
abstract class AbstractCrudController<T : OwnedResource, Cmd, C, Req, Res>(
    private val useCase: CrudUseCase<T, Cmd, C>,
    private val envelopes: ResponseEnvelopeFactory,
) {
    protected abstract fun toCommand(request: Req): Cmd

    protected abstract fun toResponse(item: T): Res

    protected abstract fun toTotalResponse(total: BigDecimal): Any

    @GetMapping
    fun list(
        @AuthenticatedUser principal: UserPrincipal,
        @RequestParam(CATEGORY_PARAM, required = false) category: C?,
    ): ApiResponse<List<Res>> = envelopes.success(useCase.list(principal.userId, category).map(::toResponse))

    @GetMapping(TOTAL_PATH)
    fun total(@AuthenticatedUser principal: UserPrincipal): ApiResponse<Any> =
        envelopes.success(toTotalResponse(useCase.total(principal.userId)))

    @GetMapping(ID_PATH)
    fun get(@AuthenticatedUser principal: UserPrincipal, @PathVariable(ID_PARAM) id: UUID): ApiResponse<Res> =
        envelopes.success(toResponse(useCase.get(principal.userId, id)))

    @PostMapping
    fun create(
        @AuthenticatedUser principal: UserPrincipal,
        @Valid @RequestBody request: Req,
    ): ResponseEntity<ApiResponse<Res>> {
        val created = useCase.create(principal.userId, toCommand(request))
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path(ID_PATH).buildAndExpand(created.id).toUri()
        return ResponseEntity.created(location).body(envelopes.success(toResponse(created)))
    }

    @PutMapping(ID_PATH)
    fun update(
        @AuthenticatedUser principal: UserPrincipal,
        @PathVariable(ID_PARAM) id: UUID,
        @Valid @RequestBody request: Req,
    ): ApiResponse<Res> = envelopes.success(toResponse(useCase.update(principal.userId, id, toCommand(request))))

    @DeleteMapping(ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticatedUser principal: UserPrincipal, @PathVariable(ID_PARAM) id: UUID) {
        useCase.delete(principal.userId, id)
    }

    private companion object {
        const val CATEGORY_PARAM = "category"
        const val TOTAL_PATH = "/total"
        const val ID_PARAM = "id"
        const val ID_PATH = "/{$ID_PARAM}"
    }
}
