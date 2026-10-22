package com.pennypincher.pro.networth.infrastructure.web.controller

import com.pennypincher.pro.networth.domain.model.SnapshotPeriod
import com.pennypincher.pro.networth.domain.port.inbound.NetWorthUseCase
import com.pennypincher.pro.networth.infrastructure.web.dto.CurrentNetWorthResponse
import com.pennypincher.pro.networth.infrastructure.web.dto.SnapshotRequest
import com.pennypincher.pro.networth.infrastructure.web.dto.SnapshotResponse
import com.pennypincher.pro.networth.infrastructure.web.mapper.NetWorthWebMapper
import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import jakarta.validation.Valid
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** `/net-worth`: live net worth and the monthly snapshot history of the caller. */
@RestController
@RequestMapping(ApiPaths.NET_WORTH)
class NetWorthController(
    private val netWorthUseCase: NetWorthUseCase,
    private val envelopes: ResponseEnvelopeFactory,
) {
    @GetMapping(CURRENT)
    fun current(@AuthenticatedUser principal: UserPrincipal): ApiResponse<CurrentNetWorthResponse> =
        envelopes.success(NetWorthWebMapper.toResponse(netWorthUseCase.getCurrentNetWorth(principal.userId)))

    /** 201 when the month had no snapshot yet, 200 when its snapshot was recalculated. */
    @PostMapping(SNAPSHOTS)
    fun saveSnapshot(
        @AuthenticatedUser principal: UserPrincipal,
        @Valid @RequestBody request: SnapshotRequest,
    ): ResponseEntity<ApiResponse<SnapshotResponse>> {
        val result = netWorthUseCase.createOrUpdateSnapshot(principal.userId, checkNotNull(request.snapshotDate))
        val status = if (result.created) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(envelopes.success(NetWorthWebMapper.toResponse(result.snapshot)))
    }

    @GetMapping(SNAPSHOTS)
    fun listSnapshots(
        @AuthenticatedUser principal: UserPrincipal,
        @RequestParam(FROM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(TO, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ApiResponse<List<SnapshotResponse>> {
        val snapshots = netWorthUseCase.listSnapshots(principal.userId, SnapshotPeriod.of(from, to))
        return envelopes.success(snapshots.map(NetWorthWebMapper::toResponse))
    }

    private companion object {
        const val CURRENT = "/current"
        const val SNAPSHOTS = "/snapshots"
        const val FROM = "from"
        const val TO = "to"
    }
}
