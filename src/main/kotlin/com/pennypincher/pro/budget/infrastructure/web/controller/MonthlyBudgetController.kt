package com.pennypincher.pro.budget.infrastructure.web.controller

import com.pennypincher.pro.budget.domain.port.inbound.MonthlyBudgetUseCase
import com.pennypincher.pro.budget.infrastructure.web.dto.CreateMonthlyBudgetRequest
import com.pennypincher.pro.budget.infrastructure.web.dto.MonthlyBudgetResponse
import com.pennypincher.pro.budget.infrastructure.web.dto.UpdateMonthlyBudgetRequest
import com.pennypincher.pro.budget.infrastructure.web.mapper.MonthlyBudgetWebMapper
import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/** `/monthly-budgets`: the caller's monthly budgets, one per month, with their calculated figures. */
@RestController
@RequestMapping(ApiPaths.MONTHLY_BUDGETS)
class MonthlyBudgetController(
    private val useCase: MonthlyBudgetUseCase,
    private val envelopes: ResponseEnvelopeFactory,
) {
    /** 201 with the `Location` of the month; 409 when that month already has a budget. */
    @PostMapping
    fun create(
        @AuthenticatedUser principal: UserPrincipal,
        @Valid @RequestBody request: CreateMonthlyBudgetRequest,
    ): ResponseEntity<ApiResponse<MonthlyBudgetResponse>> {
        val created = useCase.create(principal.userId, MonthlyBudgetWebMapper.toCommand(request))
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path(PERIOD_PATH)
            .buildAndExpand(created.period.year, created.period.month).toUri()
        return ResponseEntity.created(location).body(envelopes.success(MonthlyBudgetWebMapper.toResponse(created)))
    }

    @GetMapping
    fun list(
        @AuthenticatedUser principal: UserPrincipal,
        @RequestParam(YEAR, required = false) year: Int?,
    ): ApiResponse<List<MonthlyBudgetResponse>> =
        envelopes.success(useCase.list(principal.userId, year).map(MonthlyBudgetWebMapper::toResponse))

    @GetMapping(PERIOD_PATH)
    fun get(
        @AuthenticatedUser principal: UserPrincipal,
        @PathVariable(YEAR) year: Int,
        @PathVariable(MONTH) month: Int,
    ): ApiResponse<MonthlyBudgetResponse> =
        envelopes.success(MonthlyBudgetWebMapper.toResponse(useCase.get(principal.userId, year, month)))

    @PutMapping(ID_PATH)
    fun update(
        @AuthenticatedUser principal: UserPrincipal,
        @PathVariable(ID) id: UUID,
        @Valid @RequestBody request: UpdateMonthlyBudgetRequest,
    ): ApiResponse<MonthlyBudgetResponse> {
        val updated = useCase.update(principal.userId, id, MonthlyBudgetWebMapper.toSettings(request))
        return envelopes.success(MonthlyBudgetWebMapper.toResponse(updated))
    }

    @PostMapping(RECALCULATE_PATH)
    fun recalculate(
        @AuthenticatedUser principal: UserPrincipal,
        @PathVariable(ID) id: UUID,
    ): ApiResponse<MonthlyBudgetResponse> =
        envelopes.success(MonthlyBudgetWebMapper.toResponse(useCase.recalculate(principal.userId, id)))

    private companion object {
        const val YEAR = "year"
        const val MONTH = "month"
        const val ID = "id"
        const val PERIOD_PATH = "/{$YEAR}/{$MONTH}"
        const val ID_PATH = "/{$ID}"
        const val RECALCULATE_PATH = "$ID_PATH/recalculate"
    }
}
