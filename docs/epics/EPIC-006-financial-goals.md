# EPIC-006 — Financial Goals Module

**Epic ID**: EPIC-006
**Priority**: P2 — High-value motivational feature
**Repos**: `pennypincher-pro-backend`, `pennypincher-pro-frontend`
**Depends on**: EPIC-001, EPIC-002 complete; EPIC-003 complete (net worth used in goal progress)

## Business Value

The Metas financieras sheet defines four levels of financial independence — the "why" behind
all the careful budgeting. Showing progress toward these milestones with visual indicators
and projections transforms abstract financial numbers into a motivational roadmap.
This is the feature that makes the user want to open the app daily.

## Acceptance Criteria (Epic Level)

- User can define financial independence goals with target amounts and monthly contributions
- Progress toward each goal is calculated against current net worth
- Time-to-goal projection updates automatically as net worth changes
- All four independence levels from the Excel are pre-configured and editable
- Charts show the projected trajectory to each goal

---

## Stories

### STORY-006-01: Backend — Financial goal CRUD endpoints

**As** an authenticated user,
**I want** to manage my financial independence goals,
**So that** the app tracks my progress toward each financial milestone.

**Acceptance Criteria:**
- [ ] `GET /api/v1/financial-goals` returns all goals ordered by `targetAmount ASC`
- [ ] `POST /api/v1/financial-goals` creates goal; validates name, level, targetAmount (> 0), monthlyInvestment (>= 0), expectedAnnualReturn (0 < value <= 1)
- [ ] `PUT /api/v1/financial-goals/{id}` updates all fields
- [ ] `DELETE /api/v1/financial-goals/{id}` hard deletes (goals don't need soft delete — they are intentional milestones)
- [ ] `GoalLevel` enum: `FINANCIAL_SECURITY`, `FINANCIAL_INDEPENDENCE`, `FINANCIAL_FREEDOM`, `ABSOLUTE_FREEDOM`
- [ ] Each level has a description (for UI display — stored as constant in backend, not in DB):
  - FINANCIAL_SECURITY: "Expenses covered for 6 months without income"
  - FINANCIAL_INDEPENDENCE: "Passive income covers all basic expenses"
  - FINANCIAL_FREEDOM: "Passive income covers desired lifestyle"
  - ABSOLUTE_FREEDOM: "Unlimited financial choices"

**Financial goals from Excel (documentation for user reference):**
- Seguridad Financiera: $605,000,000 target, $1,470,000/month, 11% annual return
- Independencia Financiera: $1,016,000,000 target, $3,040,000/month, 11%
- Libertad Financiera: $2,066,000,000 target, $7,000,000/month, 11%
- Libertad Absoluta: $4,366,000,000 target, $15,850,000/month, 11%

**Technical Tasks:**
1. Create `domain/model/FinancialGoal.kt`
2. Create `domain/model/enums/GoalLevel.kt` with description constant
3. Create port interfaces, use case `FinancialGoalUseCaseImpl.kt`
4. Create JPA entity, adapter, controller, DTOs
5. Write unit + integration tests

**Commit message**: `Add financial goal management endpoints with independence level classification`

---

### STORY-006-02: Backend — Goal progress and projection calculation

**As** an authenticated user,
**I want** to see how far I am from each goal and when I'll reach it,
**So that** I can measure progress and stay motivated.

**Acceptance Criteria:**
- [ ] `GET /api/v1/financial-goals/{id}/projection` returns:
  ```json
  {
    "goalId": "uuid",
    "targetAmount": 605000000,
    "currentNetWorth": 423282773,
    "progressAmount": 423282773,
    "progressPercentage": 69.97,
    "remainingAmount": 181717227,
    "monthlyInvestment": 1470000,
    "expectedAnnualReturn": 0.11,
    "projectedMonthsToGoal": 38,
    "projectedYearsToGoal": 3.17,
    "projectedAchievementDate": "2030-01-01",
    "projectionDataPoints": [
      { "month": 1, "projectedValue": 425000000 },
      { "month": 2, "projectedValue": 427000000 },
      ...
    ]
  }
  ```
- [ ] Projection uses compound interest formula: `FV = PV * (1 + r)^n + PMT * [((1 + r)^n − 1) / r]`
  - `PV` = current net worth
  - `r` = monthly interest rate (annual / 12)
  - `n` = number of months
  - `PMT` = monthly investment
- [ ] `projectionDataPoints` shows monthly values for the projection period (max 600 months / 50 years)
- [ ] Current net worth is fetched live from `NetWorthUseCase.getCurrentNetWorth(userId)`
- [ ] `GET /api/v1/financial-goals/all-projections` returns progress for all goals in one call (for dashboard)
- [ ] Unit tests cover: goal already reached (progressPercentage = 100), zero monthly investment, various return rates

**Technical Tasks:**
1. Create `domain/service/GoalProjectionService.kt` — pure Kotlin math, no Spring
2. Implement compound interest formula with BigDecimal precision
3. Add `getProjection(goalId, userId): GoalProjection` to `FinancialGoalUseCase`
4. Create `GoalProjectionResponse.kt` DTO
5. Add `GET /financial-goals/{id}/projection` and `GET /financial-goals/all-projections` to controller
6. Write unit tests for `GoalProjectionService` with multiple scenarios

**Commit message**: `Implement compound interest projection calculation for financial goal tracking`

---

### STORY-006-03: Frontend — Financial goals screen

**As** the user,
**I want** to see all my financial goals with progress bars and time-to-goal information,
**So that** I stay motivated and can visualize how close I am to each milestone.

**Acceptance Criteria:**
- [ ] Route: `/goals`
- [ ] Four goal cards displayed vertically (or 2x2 grid on desktop)
- [ ] Each goal card shows:
  - Goal name and level badge (with distinct color per level)
  - Target amount (formatted COP)
  - Circular or linear progress indicator: current net worth / target
  - Progress percentage (large, prominent number)
  - Current net worth amount
  - Remaining amount to goal
  - Projected achievement date (month/year)
  - Monthly investment and expected return rate
- [ ] Progress indicator color: green (>= 75%), amber (50–74%), blue (25–49%), gray (< 25%)
- [ ] FAB to add a new custom goal; tap any card to edit it
- [ ] Edit/delete via bottom sheet with form
- [ ] Empty state with illustration and "Set your first financial goal" CTA

**Goal level badge colors:**
- FINANCIAL_SECURITY: Blue (`brandPrimary`)
- FINANCIAL_INDEPENDENCE: Teal (`#0D9488`)
- FINANCIAL_FREEDOM: Purple (`#7C3AED`)
- ABSOLUTE_FREEDOM: Gold (`accentGold`)

**Technical Tasks:**
1. Create `features/financial_goals/domain/entity/financial_goal_entity.dart` (Freezed)
2. Create `features/financial_goals/domain/entity/goal_projection_entity.dart` (Freezed)
3. Create repository, use cases: `GetFinancialGoals`, `GetGoalProjection`, `CreateGoal`, `UpdateGoal`, `DeleteGoal`, `GetAllProjections`
4. Create `features/financial_goals/data/` layer
5. Create `GoalsBloc` with event `GoalsLoaded`, `GoalCreated`, `GoalUpdated`, `GoalDeleted`
6. Create `features/financial_goals/presentation/screen/goals_screen.dart`
7. Create `features/financial_goals/presentation/widget/goal_card.dart`
8. Create `features/financial_goals/presentation/widget/goal_form_sheet.dart`
9. Create `features/financial_goals/presentation/widget/circular_progress_indicator_widget.dart` (custom)
10. Write BLoC unit tests

**Commit message**: `Add financial goals screen with progress tracking and projected achievement dates`

---

### STORY-006-04: Frontend — Goal projection chart

**As** the user,
**I want** to see a projected growth chart for each goal,
**So that** I can visualize the compound growth trajectory and understand the power of consistent investing.

**Acceptance Criteria:**
- [ ] Accessible from goal card via a "See projection" button or icon
- [ ] Opens a modal or navigates to `/goals/{id}/projection`
- [ ] Line chart showing projected portfolio value over time
- [ ] Horizontal dashed line at target amount
- [ ] Vertical marker where the projected line crosses the target (achievement point)
- [ ] Y axis: COP values formatted in millions
- [ ] X axis: years from now
- [ ] Current net worth shown as the starting point
- [ ] Interactive tooltip showing value and date on hover/tap
- [ ] Summary below chart: "At this rate, you'll reach [Goal Name] in [X years] on [Date]"

**Technical Tasks:**
1. Create `features/financial_goals/presentation/screen/goal_projection_screen.dart`
2. Create `features/financial_goals/presentation/widget/projection_chart.dart` (fl_chart line chart)
3. Add route `/goals/:goalId/projection` to AppRouter
4. Write widget test

**Commit message**: `Add goal projection chart with compound growth visualization and target crossing marker`

---

## Definition of Done

- [ ] All 4 stories committed
- [ ] Goal CRUD works end-to-end
- [ ] Progress percentages calculate correctly for all four goals from Excel data
- [ ] Projection dates are mathematically correct (verify with Excel formula manually)
- [ ] Charts render correctly on mobile and desktop
- [ ] Editing monthly investment amount updates the projection date in real time

## Notes

- The projection formula uses **monthly compounding** with the monthly equivalent of the annual rate:
  `monthlyRate = (1 + annualRate)^(1/12) - 1`
  This is more accurate than simply dividing by 12.
- Current net worth is always fetched live — it uses the `getCurrentNetWorth` endpoint which
  sums active assets minus active liabilities without requiring a snapshot to exist.
- The four standard goal levels from the Excel should be shown in a fixed order (SECURITY → INDEPENDENCE
  → FREEDOM → ABSOLUTE) regardless of creation order.
- Consider a "motivational moment": if the user has just passed 50% of a goal,
  show a subtle congratulatory animation on that card. This is a small delight feature,
  not a notification system — just a local UI state check.
