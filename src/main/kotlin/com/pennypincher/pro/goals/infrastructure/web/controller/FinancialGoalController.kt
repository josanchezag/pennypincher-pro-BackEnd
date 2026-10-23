package com.pennypincher.pro.goals.infrastructure.web.controller

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.FinancialGoalCommand
import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import com.pennypincher.pro.goals.domain.port.inbound.FinancialGoalUseCase
import com.pennypincher.pro.goals.infrastructure.web.dto.FinancialGoalRequest
import com.pennypincher.pro.goals.infrastructure.web.dto.FinancialGoalResponse
import com.pennypincher.pro.goals.infrastructure.web.dto.GoalLevelResponse
import com.pennypincher.pro.goals.infrastructure.web.dto.GoalProjectionResponse
import com.pennypincher.pro.goals.infrastructure.web.mapper.FinancialGoalWebMapper
import com.pennypincher.pro.goals.infrastructure.web.mapper.GoalProjectionWebMapper
import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.UncategorizedCrudController
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `/financial-goals`: the caller's goals (the CRUD endpoints come from the shared bases; `DELETE` is a hard
 * delete), the level catalog, the standard goals and the projections against the live net worth. Literal
 * paths such as `/levels` and `/all-projections` take precedence over `/{id}`.
 */
@RestController
@RequestMapping(ApiPaths.FINANCIAL_GOALS)
class FinancialGoalController(
    private val useCase: FinancialGoalUseCase,
    private val envelopes: ResponseEnvelopeFactory,
) : UncategorizedCrudController<FinancialGoal, FinancialGoalCommand, FinancialGoalRequest, FinancialGoalResponse>(
    useCase,
    envelopes,
) {
    override fun toCommand(request: FinancialGoalRequest) = FinancialGoalWebMapper.toCommand(request)

    override fun toResponse(item: FinancialGoal) = FinancialGoalWebMapper.toResponse(item)

    @GetMapping(LEVELS_PATH)
    fun levels(): ApiResponse<List<GoalLevelResponse>> = envelopes.success(GoalLevel.entries.map(GoalLevelResponse::of))

    @PostMapping(DEFAULTS_PATH)
    fun createDefaults(@AuthenticatedUser principal: UserPrincipal): ApiResponse<List<FinancialGoalResponse>> =
        envelopes.success(useCase.createDefaults(principal.userId).map(::toResponse))

    @GetMapping(PROJECTION_PATH)
    fun projection(
        @AuthenticatedUser principal: UserPrincipal,
        @PathVariable(ID_PARAM) id: UUID,
    ): ApiResponse<GoalProjectionResponse> =
        envelopes.success(GoalProjectionWebMapper.toResponse(useCase.getProjection(principal.userId, id)))

    @GetMapping(ALL_PROJECTIONS_PATH)
    fun allProjections(@AuthenticatedUser principal: UserPrincipal): ApiResponse<List<GoalProjectionResponse>> =
        envelopes.success(useCase.getAllProjections(principal.userId).map(GoalProjectionWebMapper::toResponse))

    private companion object {
        const val ID_PARAM = "id"
        const val LEVELS_PATH = "/levels"
        const val DEFAULTS_PATH = "/defaults"
        const val PROJECTION_PATH = "/{$ID_PARAM}/projection"
        const val ALL_PROJECTIONS_PATH = "/all-projections"
    }
}
