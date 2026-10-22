# EPIC-007 — Dashboard & Reports

**Epic ID**: EPIC-007
**Priority**: P2 — High-value overview and insight feature
**Repos**: `pennypincher-pro-backend`, `pennypincher-pro-frontend`
**Depends on**: EPIC-003, EPIC-004, EPIC-005, EPIC-006 complete (all data modules needed)

## Business Value

The dashboard is the first screen the user sees after login. It must answer "how am I doing
financially right now?" in under 5 seconds. A well-designed dashboard reduces the need to navigate
to every sub-section for a quick status check — one glance shows net worth, cash flow, goal
progress, and any unusual expense patterns.

## Acceptance Criteria (Epic Level)

- Dashboard loads complete financial overview in a single API call
- Key metrics visible without scrolling on any screen size
- Monthly comparison shows current vs previous period
- Net worth trend chart visible on the dashboard
- All goal progress visible as compact indicators
- The dashboard feels fast (data loads under 2 seconds on Render free tier after warm-up)

---

## Stories

### STORY-007-01: Backend — Dashboard summary endpoint

**As** the Flutter frontend,
**I want** a single API endpoint that returns all data needed for the dashboard,
**So that** the app only needs one HTTP call to render the complete overview.

**Acceptance Criteria:**
- [ ] `GET /api/v1/dashboard/summary` returns a comprehensive summary DTO
- [ ] Response structure:
  ```json
  {
    "data": {
      "netWorth": {
        "current": 423282773,
        "previousMonth": 418000000,
        "delta": 5282773,
        "deltaPercentage": 1.26,
        "totalAssets": 526932742,
        "totalLiabilities": 103649969
      },
      "currentMonthBudget": {
        "year": 2026,
        "month": 10,
        "totalIncome": 37152922,
        "totalExpenses": 19135201,
        "monthlyCashFlow": 18017721,
        "cashFlowPercentage": 48.49,
        "exists": true
      },
      "expenseSummary": {
        "totalCreditCardPayments": 0,
        "totalLoanPayments": 3280231,
        "totalFixedExpenses": 13357550,
        "totalVariableExpenses": 2497420,
        "totalSavingsContributions": 0
      },
      "goalsSummary": [
        {
          "goalId": "uuid",
          "name": "Seguridad Financiera",
          "level": "FINANCIAL_SECURITY",
          "progressPercentage": 69.97,
          "projectedAchievementDate": "2030-01-01"
        }
      ],
      "recentNetWorthSnapshots": [
        { "snapshotDate": "2026-10-01", "netWorth": 423282773 },
        ...
      ],
      "userProfile": {
        "name": "Josimar",
        "pictureUrl": "https://..."
      }
    }
  }
  ```
- [ ] `recentNetWorthSnapshots`: last 12 months ordered ASC
- [ ] `goalsSummary`: all goals with current progress
- [ ] Response time < 500ms (all queries run in parallel using Kotlin coroutines or CompletableFuture)
- [ ] Integration test verifying correct aggregation

**Technical Tasks:**
1. Create `domain/port/inbound/DashboardUseCase.kt`: `getSummary(userId): DashboardSummary`
2. Create `application/usecase/DashboardUseCaseImpl.kt`:
   - Fetches all required data using existing use cases/repositories
   - Uses `CompletableFuture.allOf(...)` to parallelize the queries
3. Create `DashboardSummaryResponse.kt` with nested DTOs
4. Create `DashboardController.kt`
5. Write integration test

**Commit message**: `Add dashboard summary endpoint with parallel data aggregation`

---

### STORY-007-02: Backend — Net worth trend report endpoint

**As** the frontend,
**I want** a report endpoint with historical net worth data for a date range,
**So that** I can render detailed trend charts with flexible time ranges.

**Acceptance Criteria:**
- [ ] `GET /api/v1/reports/net-worth-trend?from=2021-10-01&to=2026-10-31`
- [ ] Returns all snapshots within range ordered by date ASC
- [ ] If no snapshots exist in range: returns empty array (not 404)
- [ ] Each point: `{ snapshotDate, totalAssets, totalLiabilities, netWorth, monthOverMonthDelta }`
- [ ] `monthOverMonthDelta` = netWorth − previous snapshot's netWorth (null for first point)
- [ ] Optional `?groupBy=quarter` or `?groupBy=year` for aggregated views (average per period)

**Technical Tasks:**
1. Add `getTrendReport(userId, from, to): List<TrendPoint>` to `NetWorthUseCase`
2. Create `TrendPointResponse.kt`
3. Add endpoint to `NetWorthController.kt` (or new `ReportController.kt`)
4. Write unit test for month-over-month delta calculation

**Commit message**: `Add net worth trend report endpoint with configurable date range`

---

### STORY-007-03: Backend — Monthly comparison report

**As** the frontend,
**I want** a comparison of the current month's budget vs the previous N months,
**So that** I can identify spending trends and anomalies.

**Acceptance Criteria:**
- [ ] `GET /api/v1/reports/monthly-comparison?months=6`
- [ ] Returns last N monthly budgets (including current if exists) ordered by date ASC
- [ ] Each entry: `{ year, month, totalIncome, totalExpenses, monthlyCashFlow, cashFlowPercentage }`
- [ ] `cashFlowPercentage` = (monthlyCashFlow / totalIncome) * 100
- [ ] Missing months (no budget created) are included as null entries to maintain the timeline

**Technical Tasks:**
1. Add `getMonthlyComparison(userId, months): List<MonthlyComparisonPoint>` to `MonthlyBudgetUseCase`
2. Logic: query last N months; fill gaps with null values
3. Create `MonthlyComparisonResponse.kt`
4. Add endpoint to controller
5. Write unit test

**Commit message**: `Add monthly budget comparison report for trend analysis`

---

### STORY-007-04: Frontend — Main dashboard screen

**As** the user,
**I want** a comprehensive dashboard that summarizes my entire financial situation,
**So that** I can understand my financial health at a glance without navigating to multiple screens.

**Acceptance Criteria:**
- [ ] Route: `/dashboard` — first screen after login
- [ ] Top bar: app name/logo, user avatar (from Google), greeting ("Good morning, Josimar")
- [ ] **Section 1 — Net Worth Overview** (top section):
  - Large KPI: Current Net Worth with delta vs last month (arrow + %, green/red)
  - Two secondary KPIs: Total Assets | Total Liabilities
  - Mini net worth sparkline (last 6 months)
- [ ] **Section 2 — This Month's Budget**:
  - If budget exists: Income | Expenses | Cash Flow cards
  - If no budget: "Create this month's budget" action card
  - Cash flow indicator (green if positive)
- [ ] **Section 3 — Financial Goals** (compact):
  - Horizontal scroll of goal progress cards (or 2x2 grid on desktop)
  - Each: goal name, circular mini-progress, percentage
  - Tap navigates to `/goals`
- [ ] **Section 4 — Quick Actions**:
  - Buttons: "Record Snapshot", "Add Expense", "View Report"
- [ ] Pull-to-refresh (web: refresh button) reloads all data
- [ ] All sections show loading skeletons while data loads
- [ ] Entire dashboard renders from a single API call to `/dashboard/summary`

**Layout (desktop):**
```
┌─────────────────────────────────────────────────────┐
│  Net Worth (large)  │  Budget Summary                │
│  Assets | Liabs     │  Income | Expenses | CashFlow  │
│  Sparkline chart    │                                 │
├─────────────────────┴────────────────────────────────┤
│  Goals (4 cards in a row)                            │
├──────────────────────────────────────────────────────┤
│  Quick Actions (4 buttons)                           │
└──────────────────────────────────────────────────────┘
```

**Layout (mobile):**
Single-column, sections stacked vertically.

**Technical Tasks:**
1. Create `features/dashboard/domain/entity/dashboard_summary_entity.dart` (Freezed)
2. Create `GetDashboardSummary` use case
3. Create `features/dashboard/data/` layer
4. Create `DashboardBloc` with `DashboardLoaded`, `DashboardRefreshed`, `DashboardError` events/states
5. Create `features/dashboard/presentation/screen/dashboard_screen.dart`
6. Create `features/dashboard/presentation/widget/net_worth_summary_card.dart`
7. Create `features/dashboard/presentation/widget/budget_summary_card.dart`
8. Create `features/dashboard/presentation/widget/goal_progress_chip.dart`
9. Create `features/dashboard/presentation/widget/quick_actions_row.dart`
10. Create `features/dashboard/presentation/widget/mini_sparkline.dart` (small fl_chart)
11. Write BLoC unit tests

**Commit message**: `Build main dashboard screen with complete financial overview in a single view`

---

### STORY-007-05: Frontend — Net worth detailed report screen

**As** the user,
**I want** a detailed report screen with the full net worth history chart,
**So that** I can see every month of my wealth-building journey since October 2021.

**Acceptance Criteria:**
- [ ] Accessible from `/net-worth` or quick action on dashboard
- [ ] Full-width line chart showing complete history
- [ ] Time range selector: 6M | 1Y | 3Y | All
- [ ] Chart shows net worth line + assets area + liabilities area (stacked area chart)
- [ ] Summary table below chart: month | assets | liabilities | net worth | delta
- [ ] Highlight best month (highest net worth) and show delta from start

**Technical Tasks:**
1. Create `GetNetWorthTrend` use case in frontend
2. Create `features/net_worth/presentation/screen/net_worth_report_screen.dart`
3. Create `features/net_worth/presentation/widget/full_history_chart.dart`
4. Create `features/net_worth/presentation/widget/time_range_selector.dart` (reusable)
5. Write widget tests

**Commit message**: `Add detailed net worth report with full history chart and time range selection`

---

### STORY-007-06: Frontend — Monthly budget comparison screen

**As** the user,
**I want** to compare my last 6 months of budgets side by side,
**So that** I can identify income and expense trends and see if my financial habits are improving.

**Acceptance Criteria:**
- [ ] Accessible from budget section or reports
- [ ] Bar chart: 6 bars (one per month), each split: income (blue) vs expenses (amber)
- [ ] Line overlay showing monthly cash flow trend
- [ ] Summary table below: month | income | expenses | cash flow | CF%
- [ ] Months with no budget shown as empty/zero bars
- [ ] Color code cash flow column: green if > 30% of income, amber if 10–30%, red if < 10%

**Technical Tasks:**
1. Create `GetMonthlyComparison` use case
2. Create `features/budget/presentation/screen/budget_comparison_screen.dart`
3. Create `features/budget/presentation/widget/budget_comparison_chart.dart` (fl_chart BarChart)
4. Write tests

**Commit message**: `Add monthly budget comparison screen with income vs expense bar chart`

---

## Definition of Done

- [ ] All 6 stories committed
- [ ] Dashboard loads in < 2 seconds on warm Render instance
- [ ] All dashboard sections populate correctly with real data
- [ ] Charts render on mobile (375px), tablet (768px), desktop (1280px)
- [ ] Net worth report shows complete history
- [ ] Budget comparison shows correct 6-month trend
- [ ] Pull-to-refresh works and updates all sections

## Notes

- The dashboard summary endpoint uses parallel queries to minimize response time.
  On the free tier PostgreSQL (Neon), individual queries should each complete in < 100ms
  for the small dataset (personal use). Parallel execution brings total to ~150ms.
- If the user has no data yet (first use), the dashboard should show onboarding prompts:
  "Start by adding your assets" → link to /net-worth/assets
  This replaces the empty state KPIs with actionable guidance.
- The greeting uses the user's first name extracted from `userProfile.name`. Split on space, take first element.
- Time-of-day greeting: "Good morning" (5am–12pm), "Good afternoon" (12pm–6pm), "Good evening" (6pm–5am)
