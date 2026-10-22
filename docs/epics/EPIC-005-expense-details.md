# EPIC-005 — Expense Details Module

**Epic ID**: EPIC-005
**Priority**: P1 — Core financial tracking
**Repos**: `pennypincher-pro-backend`, `pennypincher-pro-frontend`
**Depends on**: EPIC-001, EPIC-002, EPIC-004 complete (budget totals depend on expense data)

## Business Value

The DETALLES 30 sheet is the most granular and data-rich part of the financial workflow.
It breaks down every monthly peso spent across 5 categories: credit cards, loans, fixed expenses,
variable expenses, and savings. Digitizing this eliminates manual summing and enables drill-down
views from the budget percentages to the individual line items.

## Acceptance Criteria (Epic Level)

- User can manage credit cards with their monthly payment and total balance
- User can manage loans with monthly installments and remaining balance
- User can manage fixed monthly expenses by category
- User can manage variable monthly expenses by category
- User can track savings accounts and contributions
- All expense totals feed automatically into the monthly budget calculations

---

## Stories

### STORY-005-01: Backend — Credit card CRUD endpoints

**As** an authenticated user,
**I want** to manage my credit cards with monthly payment amounts,
**So that** I can track what portion of my income goes to credit card debt.

**Acceptance Criteria:**
- [ ] `GET /api/v1/credit-cards` returns all active credit cards ordered by name
- [ ] `POST /api/v1/credit-cards` creates card; validates name (required), monthlyPayment (>= 0), totalBalance (>= 0)
- [ ] `PUT /api/v1/credit-cards/{id}` updates; `404` for wrong user
- [ ] `DELETE /api/v1/credit-cards/{id}` soft deletes
- [ ] `GET /api/v1/credit-cards/summary` returns `{ totalMonthlyPayments, totalOutstandingBalance }`

**Credit cards in the system (from Excel — documentation only, user enters them):**
- Tarjeta Nu Bank: $0 monthly / $12,407,700 balance
- Tarjeta Banco de Occidente: $0 monthly / $30,049,222 balance
- Tarjeta Nu Bank Yasmin: $0 monthly / $500,000 balance
- Addi Josi: $0 monthly / $0 balance
- Addi Yasmin: $0 monthly / $0 balance

**Technical Tasks:**
1. Create `domain/model/CreditCard.kt`
2. Create port interfaces, use case `CreditCardUseCaseImpl.kt`
3. Create JPA entity, adapter, controller, DTOs
4. Write unit + integration tests

**Commit message**: `Add credit card management endpoints with monthly payment and balance tracking`

---

### STORY-005-02: Backend — Loan CRUD endpoints

**As** an authenticated user,
**I want** to manage my bank and personal loans,
**So that** I know exactly how much of my income goes to debt repayment.

**Acceptance Criteria:**
- [ ] Same CRUD pattern as credit cards
- [ ] `GET /api/v1/loans/summary` returns `{ totalMonthlyPayments, totalOutstandingBalance }`
- [ ] Fields: `name`, `monthlyPayment`, `totalBalance`, `isActive`

**Loans from Excel (documentation only):**
- Prestamo Occidente 190028884: $1,258,908/month, $88,614,841 balance
- Prestamo Davivienda 7141: $1,339,823/month, $59,781,329 balance
- Prestamo Davivienda 2670: $681,500/month, $22,727,489 balance

**Technical Tasks:** (mirror of STORY-005-01)

**Commit message**: `Add loan management endpoints with installment and balance tracking`

---

### STORY-005-03: Backend — Fixed expense CRUD endpoints

**As** an authenticated user,
**I want** to manage my recurring fixed monthly expenses,
**So that** I have a complete picture of my mandatory monthly outflows.

**Acceptance Criteria:**
- [ ] `GET /api/v1/fixed-expenses` returns all active fixed expenses ordered by `category`, then `name`
- [ ] Filter: `?category=EDUCATION`
- [ ] `POST /api/v1/fixed-expenses` validates name, monthlyAmount (>= 0), category
- [ ] `GET /api/v1/fixed-expenses/summary` returns `{ totalMonthlyAmount }` and total by category

**ExpenseCategory enum (fixed)**: `FOOD`, `EDUCATION`, `TRANSPORT`, `UTILITIES`, `INSURANCE`, `HOUSING`,
`HEALTH`, `CHARITY`, `COMMUNICATION`, `TAXES`, `OTHER`

**Fixed expenses from Excel:**
- Gasolina Carro: $450,000 (TRANSPORT)
- Mercado: $1,100,000 (FOOD)
- Tigo San Javier: $107,000 (COMMUNICATION)
- EPM: $333,100 (UTILITIES)
- Poliza Autos: $321,388 (INSURANCE)
- Colegio Thiana: $2,271,042 (EDUCATION)
- Donación Aldeas Colombia: $50,000 (CHARITY)
- Celular Josimar: $44,600 (COMMUNICATION)
- Celular Yasmin: $41,000 (COMMUNICATION)
- Arriendo San Javier: $2,543,000 (HOUSING)
- Plan Dental Axa: $0 (HEALTH)
- Retencion en la fuente: $1,764,000 (TAXES)
- Descuentos S&P: $1,620,000 (OTHER)
- Poliza Salud: $1,382,421 (INSURANCE)
- Impuesto Carro: $147,000 (TAXES)
- SOAT: $80,124 (INSURANCE)
- Tecnomecanica: $26,208 (TRANSPORT)
- Natación Thiana: $76,667 (EDUCATION)
- Clases BioRepro Yasmin: $1,000,000 (HEALTH)

**Technical Tasks:** (mirror pattern + category-based summary logic)

**Commit message**: `Add fixed expense management endpoints with category breakdown summary`

---

### STORY-005-04: Backend — Variable expense CRUD endpoints

**As** an authenticated user,
**I want** to manage discretionary and variable monthly expenses,
**So that** I can budget for flexible spending categories.

**Acceptance Criteria:**
- [ ] Same CRUD pattern
- [ ] `GET /api/v1/variable-expenses/summary` returns `{ totalMonthlyAmount }`

**VariableExpenseCategory enum**: `LEISURE`, `STREAMING`, `HOUSEHOLD_STAFF`, `DINING`, `CLOTHING`, `GIFTS`, `OTHER`

**Variable expenses from Excel:**
- Ocio Josi: $500,000 (LEISURE)
- Ocio Yas: $1,000,000 (LEISURE)
- Ocio Casa: $500,000 (LEISURE)
- Spotify: $0 (STREAMING)
- Netflix: $42,000 (STREAMING)
- DisneyPlus: $47,420 (STREAMING)
- Doña Gladys: $360,000 (HOUSEHOLD_STAFF)
- Motilada: $48,000 (HOUSEHOLD_STAFF)

**Commit message**: `Add variable expense management endpoints with discretionary spending tracking`

---

### STORY-005-05: Backend — Savings account CRUD endpoints

**As** an authenticated user,
**I want** to track my savings accounts and their balances,
**So that** I can see how much I have saved and monitor my savings rate.

**Acceptance Criteria:**
- [ ] `GET /api/v1/savings-accounts` returns all active savings accounts
- [ ] `POST /api/v1/savings-accounts` validates name, monthlyContribution (>= 0), currentBalance (>= 0)
- [ ] `GET /api/v1/savings-accounts/summary` returns `{ totalMonthlyContributions, totalCurrentBalance }`

**Savings from Excel:**
- Licor Importado: $0/month, $15,000,000 balance
- Colchon Licor: $0/month, $56,000,000 balance
- Cash USD: $0/month, $5,082,000 balance
- Cuenta Facebank: $0/month, $0 balance

**Commit message**: `Add savings account management endpoints with contribution and balance tracking`

---

### STORY-005-06: Frontend — Expenses navigation hub and credit cards screen

**As** the user,
**I want** a well-organized expenses section with sub-screens for each expense type,
**So that** I can navigate easily between different expense categories.

**Acceptance Criteria:**
- [ ] Route: `/expenses` — hub page showing summary cards for each expense type
  - 5 summary cards: Credit Cards, Loans, Fixed Expenses, Variable Expenses, Savings
  - Each card shows: total monthly amount and quick navigation link
- [ ] Route: `/expenses/credit-cards`:
  - List of cards showing name, monthly payment, total balance
  - Summary row at bottom: total monthly payments | total outstanding balance
  - FAB to add; tap to edit in bottom sheet; swipe to delete with confirmation
  - Empty state
- [ ] Responsive layout

**Technical Tasks:**
1. Create `features/expense_details/domain/entity/credit_card_entity.dart`
2. Create repository, use cases, data layer for credit cards
3. Create `CreditCardBloc`
4. Create `expenses_hub_screen.dart` with summary cards
5. Create `credit_cards_screen.dart`
6. Create `credit_card_form_sheet.dart`
7. Write BLoC unit tests

**Commit message**: `Add expenses hub and credit cards management screen`

---

### STORY-005-07: Frontend — Loans screen

**As** the user,
**I want** a screen to manage my loan installments and outstanding balances,
**So that** I can track my debt repayment progress.

**Acceptance Criteria:**
- [ ] Route: `/expenses/loans`
- [ ] Shows each loan: name, monthly installment, remaining balance
- [ ] Progress indicator showing balance reduction potential (remaining balance / original if tracked)
- [ ] Summary: total monthly payments, total outstanding debt
- [ ] FAB, edit, delete with confirmation
- [ ] Empty state

**Technical Tasks:** (mirror credit cards screen for loans)

**Commit message**: `Add loans management screen with installment and balance tracking`

---

### STORY-005-08: Frontend — Fixed and variable expenses screens

**As** the user,
**I want** dedicated screens for fixed and variable expenses,
**So that** I can manage each type of recurring cost separately.

**Acceptance Criteria:**
- [ ] Route: `/expenses/fixed` — fixed expenses grouped by category with category subtotals
- [ ] Route: `/expenses/variable` — variable expenses grouped by category
- [ ] Both screens: FAB to add, tap to edit, swipe/button to delete
- [ ] Category chips as filters at the top (tap to filter by category)
- [ ] Summary card at top showing total monthly amount
- [ ] Empty states per category and overall

**Technical Tasks:**
1. Create entities, repositories, use cases for fixed and variable expenses
2. Create `FixedExpenseBloc` and `VariableExpenseBloc`
3. Create `fixed_expenses_screen.dart` and `variable_expenses_screen.dart`
4. Create shared `expense_item_tile.dart` reusable widget
5. Create `expense_form_sheet.dart` (parameterized for both types)
6. Write BLoC unit tests

**Commit message**: `Add fixed and variable expense screens with category grouping and filtering`

---

### STORY-005-09: Frontend — Savings accounts screen

**As** the user,
**I want** a screen to track all my savings and their current balances,
**So that** I can see my liquid reserves at a glance.

**Acceptance Criteria:**
- [ ] Route: `/expenses/savings`
- [ ] Shows savings accounts: name, monthly contribution, current balance
- [ ] Total savings balance shown prominently (green `StatCard`)
- [ ] Total monthly contributions shown
- [ ] FAB to add, tap to edit, delete with confirmation
- [ ] Empty state encouraging first savings entry

**Technical Tasks:** (mirror pattern for savings)

**Commit message**: `Add savings accounts management screen with balance and contribution tracking`

---

## Definition of Done

- [ ] All 9 stories committed
- [ ] All expense endpoints return correct filtered data with user isolation
- [ ] Expense totals flow correctly into monthly budget recalculation
- [ ] All 5 expense sub-screens navigate correctly from the hub
- [ ] Summary cards on the hub show live totals
- [ ] CRUD operations work on all 5 expense types

## Notes

- The `expenses/savings` route is under expenses navigation for UI grouping, even though savings
  are logically different from expenses. This mirrors the Excel's DETALLES 30 layout.
- Variable expense amounts are budgeted estimates, not actual transactions. Actual transaction
  tracking is a future enhancement (out of scope for Phase 1).
- Category chips filter is a nice UX touch that makes it easy to find specific expenses
  in a long list — especially for fixed expenses which can have 15+ line items.
