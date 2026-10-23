package com.pennypincher.pro.goals.infrastructure.web.controller

import com.pennypincher.pro.goals.domain.model.FinancialGoal
import com.pennypincher.pro.goals.domain.model.FinancialGoalCommand
import com.pennypincher.pro.goals.domain.model.enums.GoalLevel
import com.pennypincher.pro.goals.domain.port.inbound.FinancialGoalUseCase
import com.pennypincher.pro.goals.infrastructure.web.dto.FinancialGoalRequest
import com.pennypincher.pro.goals.infrastructure.web.dto.FinancialGoalResponse
import com.pennypincher.pro.goals.infrastructure.web.dto.GoalLevelResponse
import com.pennypincher.pro.goals.infrastructure.web.mapper.FinancialGoalWebMapper
import com.pennypincher.pro.shared.domain.model.UserPrincipal
import com.pennypincher.pro.shared.infrastructure.web.ApiPaths
import com.pennypincher.pro.shared.infrastructure.web.ResponseEnvelopeFactory
import com.pennypincher.pro.shared.infrastructure.web.controller.UncategorizedCrudController
import com.pennypincher.pro.shared.infrastructure.web.dto.ApiResponse
import com.pennypincher.pro.shared.infrastructure.web.resolver.AuthenticatedUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `/financial-goals`: the caller's goals (the CRUD endpoints come from the shared bases; `DELETE` is a hard
 * delete), the level catalog and the standard goals. Literal paths such as `/levels` take precedence over
 * `/{id}`.
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

    private companion object {
        const val LEVELS_PATH = "/levels"
        const val DEFAULTS_PATH = "/defaults"
    }
}
