# EPIC-004 — Budget Module

**Epic ID**: EPIC-004
**Priority**: P1 — Core financial tracking
**Repos**: `pennypincher-pro-backend`, `pennypincher-pro-frontend`
**Depends on**: EPIC-001, EPIC-002 complete

## Business Value

The monthly budget declaration (PRESUPUESTO 30 sheet) is the most-used view in the user's
financial workflow. It gives a high-level picture of income vs expenses and shows where money
goes as percentage of total income. Digitizing this removes the manual calculation burden
and provides instant cash flow visibility.

## Acceptance Criteria (Epic Level)

- User can manage recurring income sources with categories
- User can declare a monthly budget for any month, with automated calculations
- Cash flow (income − expenses) is calculated and displayed automatically
- Expense category percentages are shown vs ideal targets
- User can navigate between months to see historical budgets

---

## Stories

### STORY-004-01: Backend — Income source CRUD endpoints

**As** an authenticated user,
**I want** to manage my recurring income sources,
**So that** the app knows all my income streams and can calculate my total monthly income accurately.

**Acceptance Criteria:**
- [ ] `GET /api/v1/income-sources` returns all active income sources ordered by `category`, then `name`
- [ ] `POST /api/v1/income-sources` creates income source; validates name (required), amount (>= 0), category
- [ ] `PUT /api/v1/income-sources/{id}` updates; returns `404` for wrong user
- [ ] `DELETE /api/v1/income-sources/{id}` soft deletes
- [ ] `GET /api/v1/income-sources/total` returns `{ totalMonthlyIncome: BigDecimal }`

**IncomeCategory enum**: `EMPLOYMENT`, `RENTAL`, `BUSINESS`, `INVESTMENT`, `FREELANCE`, `OTHER`

**Income sources from Excel to pre-define (as data examples, not seeded data):**
- AvenueCode (EMPLOYMENT)
- Remoti (EMPLOYMENT)
- Arriendo Madera (RENTAL)
- Licor (BUSINESS)
- Airbnb Barranca (RENTAL)
- Suasie (BUSINESS)

**Technical Tasks:**
1. Create `domain/model/IncomeSource.kt`
2. Create `domain/model/enums/IncomeCategory.kt`
3. Create port interfaces, use case, JPA entity, adapter, controller, DTOs
4. Write unit + integration tests

**Commit message**: `Add income source management endpoints with category classification`

---

### STORY-004-02: Backend — Monthly budget CRUD and cash flow calculation

**As** an authenticated user,
**I want** to create and manage my monthly budget record,
**So that** I can declare my income, track expenses, and see my monthly cash flow.

**Acceptance Criteria:**
- [ ] `POST /api/v1/monthly-budgets` creates budget for a `{ year, month }`
  - `totalIncome` auto-calculated from active income sources (sum of amounts)
  - `totalExpenses` auto-calculated from credit cards + loans + fixed + variable expenses (sum of monthly amounts)
  - `monthlyCashFlow` = totalIncome − totalExpenses
  - Expense category percentages calculated automatically
  - Unique constraint: one budget per user per year/month
- [ ] `GET /api/v1/monthly-budgets` returns all budgets for the user ordered by year/month DESC
  - Optional filter: `?year=2026`
- [ ] `GET /api/v1/monthly-budgets/{year}/{month}` returns specific month or `404`
- [ ] `PUT /api/v1/monthly-budgets/{id}` allows manual override of `exchangeRate`, `workDays`; recalculates totals
- [ ] `POST /api/v1/monthly-budgets/{id}/recalculate` forces recalculation from current income/expense data
- [ ] Returns `409` if budget for that month already exists on `POST`

**Percentage calculation logic:**
```
creditCardPercentage = totalCreditCardPayments / totalIncome
loanPercentage       = totalLoanPayments / totalIncome
expensePercentage    = totalMonthlyExpenses / totalIncome
savingsPercentage    = totalSavingsContributions / totalIncome
charityPercentage    = charityDonations / totalIncome (from fixed expenses with category CHARITY)
```

**Ideal targets (from Excel — for display comparison only, not enforced):**
```
creditCardPercentage ideal: 20%
business/investment ideal:  60%
savingsPercentage ideal:    10%
charityPercentage ideal:    10%
```

**Technical Tasks:**
1. Create `domain/model/MonthlyBudget.kt`
2. Create `domain/service/BudgetCalculationService.kt` — pure domain logic, no Spring annotations
3. Create port interfaces, use case `MonthlyBudgetUseCaseImpl.kt`
4. Create JPA entity, adapter, controller
5. Create `MonthlyBudgetRequest.kt`, `MonthlyBudgetResponse.kt` (include all calculated fields)
6. Write unit tests for `BudgetCalculationService` with multiple scenarios

**Commit message**: `Implement monthly budget creation with automatic cash flow and percentage calculations`

---

### STORY-004-03: Frontend — Income sources management screen

**As** the user,
**I want** a screen to manage all my recurring income streams,
**So that** I can keep my income profile accurate as my income sources change.

**Acceptance Criteria:**
- [ ] Route: `/budget/income`
- [ ] List of income sources grouped by category
- [ ] Each item: name, category chip, monthly amount (COP formatted)
- [ ] Total monthly income shown as `StatCard` at top
- [ ] FAB to add new income source
- [ ] Tap to edit in bottom sheet; delete with confirmation
- [ ] Empty state with encouraging message ("Add your first income source")
- [ ] Responsive list layout

**Technical Tasks:**
1. Create `features/budget/domain/entity/income_source_entity.dart` (Freezed)
2. Create `features/budget/domain/repository/income_source_repository.dart`
3. Create use cases: `GetIncomeSources`, `CreateIncomeSource`, `UpdateIncomeSource`, `DeleteIncomeSource`
4. Create `features/budget/data/` layer
5. Create `features/budget/presentation/bloc/income_source_bloc.dart`
6. Create `features/budget/presentation/screen/income_sources_screen.dart`
7. Create `income_source_form_sheet.dart`
8. Write BLoC unit tests

**Commit message**: `Add income sources management screen with category grouping`

---

### STORY-004-04: Frontend — Monthly budget overview screen

**As** the user,
**I want** a monthly budget overview screen,
**So that** I can see my income vs expenses for any month and understand where my money goes.

**Acceptance Criteria:**
- [ ] Route: `/budget/overview`
- [ ] Month/year selector at the top (previous ← [Month Year] → next)
- [ ] Top stat row: Total Income, Total Expenses, Cash Flow
  - Cash Flow card: green if positive, red if negative
- [ ] Expense breakdown section showing each category:
  - Row: category label | % actual | progress bar | % ideal target
  - Color coding: green if at or below ideal, amber if 10% over, red if significantly over
  - Categories: Credit Cards, Loans, Monthly Expenses, Savings, Charity
- [ ] "Create Budget" button for the current month if no budget exists
- [ ] Sub-section showing income sources contributing to the total
- [ ] TRM and work days editable inline (small edit icon next to values)
- [ ] Responsive: stat cards stack on mobile; 2-column layout on desktop

**Technical Tasks:**
1. Create `features/budget/domain/entity/monthly_budget_entity.dart` (Freezed)
2. Create use cases: `GetMonthlyBudget`, `CreateMonthlyBudget`, `UpdateMonthlyBudget`, `RecalculateBudget`
3. Create `features/budget/presentation/bloc/monthly_budget_bloc.dart` with month navigation events
4. Create `features/budget/presentation/screen/budget_overview_screen.dart`
5. Create `features/budget/presentation/widget/expense_percentage_row.dart`
6. Create `features/budget/presentation/widget/month_selector.dart` (reusable)
7. Write BLoC unit tests

**Commit message**: `Add monthly budget overview with expense percentage analysis and month navigation`

---

## Definition of Done

- [ ] All 4 stories committed
- [ ] Income sources CRUD works end-to-end
- [ ] Monthly budget creation calculates correct totals
- [ ] Navigating between months shows correct budget for each month
- [ ] Percentage progress bars display correctly on all screen sizes
- [ ] Cash flow shows positive in green, negative in red

## Notes

- The budget calculation always reads from the current state of income/expense records.
  If the user changes an income source, they should use "Recalculate" to update the budget.
- Exchange rate (TRM) has no auto-fetch — the user enters it manually as it was in the Excel.
  Future enhancement: connect to a COP/USD exchange rate API.
- Work days field is informational only (used for reference in the Excel to calculate hourly rate context).
