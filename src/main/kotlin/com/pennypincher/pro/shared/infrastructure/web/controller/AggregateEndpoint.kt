package com.pennypincher.pro.shared.infrastructure.web.controller

import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping

/** Opt-in `GET /total` of a CRUD controller, implemented by delegation to an [AggregateEndpoint]. */
interface TotalEndpoint<R> {
    @GetMapping("/total")
    fun total(@AuthenticatedUser principal: UserPrincipal): ApiResponse<R>
}

/** Opt-in `GET /summary` of a CRUD controller, implemented by delegation to an [AggregateEndpoint]. */
interface SummaryEndpoint<R> {
    @GetMapping("/summary")
    fun summary(@AuthenticatedUser principal: UserPrincipal): ApiResponse<R>
}

/**
 * Answers an aggregate of the caller's records: [aggregate] computes it and [toResponse] turns it into the
 * HTTP body. It implements both [TotalEndpoint] and [SummaryEndpoint], but a controller must delegate
 * exactly one of them, for example `SummaryEndpoint<R> by AggregateEndpoint(envelopes, useCase::summary, ::Response)`.
 * Spring reads the mapping annotation from the delegated interface, so only that endpoint is exposed;
 * delegating both would expose `/total` and `/summary` with the same body.
 */
class AggregateEndpoint<S, R>(
    private val envelopes: ResponseEnvelopeFactory,
    private val aggregate: (userId: UUID) -> S,
    private val toResponse: (S) -> R,
) : TotalEndpoint<R>,
    SummaryEndpoint<R> {
    override fun total(principal: UserPrincipal): ApiResponse<R> = respond(principal)

    override fun summary(principal: UserPrincipal): ApiResponse<R> = respond(principal)

    private fun respond(principal: UserPrincipal): ApiResponse<R> =
        envelopes.success(toResponse(aggregate(principal.userId)))
}
