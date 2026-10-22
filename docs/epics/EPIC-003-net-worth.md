# EPIC-003 — Net Worth Module

**Epic ID**: EPIC-003
**Priority**: P1 — Core financial tracking
**Repos**: `pennypincher-pro-backend`, `pennypincher-pro-frontend`
**Depends on**: EPIC-001, EPIC-002 complete

## Business Value

Net worth tracking is the cornerstone metric of personal finance. The user has maintained this
history since October 2021 — digitizing it with charts and trends replaces the most critical
Excel sheet (FORTUNA NETA) and provides visual evidence of wealth-building progress over time.

## Acceptance Criteria (Epic Level)

- User can manage their list of assets (what they own) with current values
- User can manage their list of liabilities (what they owe) with current balances
- User can create a monthly snapshot that calculates net worth = assets − liabilities
- A historical chart shows net worth evolution month by month
- Current net worth is always visible on the net worth overview page

---

## Stories

### STORY-003-01: Backend — Asset CRUD endpoints

**As** an authenticated user,
**I want** API endpoints to create, read, update, and soft-delete my assets,
**So that** I can maintain an accurate list of everything I own and its current value.

**Acceptance Criteria:**
- [ ] `GET /api/v1/assets` returns all active assets for the authenticated user, sorted by `category`, then `name`
- [ ] `GET /api/v1/assets?category=REAL_ESTATE` filters by category
- [ ] `POST /api/v1/assets` creates a new asset; validates name (required), value (>= 0), category (valid enum)
- [ ] `PUT /api/v1/assets/{id}` updates name, value, category; returns `404` if not found or belongs to another user
- [ ] `DELETE /api/v1/assets/{id}` soft-deletes (sets `is_active = false`); returns `204`
- [ ] `GET /api/v1/assets/total` returns `{ totalAssets: BigDecimal }` — sum of all active asset values
- [ ] All endpoints require authentication
- [ ] Unit tests for use case; integration tests for controller with Testcontainers

**AssetCategory enum**: `REAL_ESTATE`, `VEHICLE`, `BUSINESS`, `CASH`, `SAVINGS`, `EQUIPMENT`, `OTHER`

**Technical Tasks:**
1. Create `domain/model/Asset.kt`
2. Create `domain/model/enums/AssetCategory.kt`
3. Create `domain/port/outbound/AssetRepository.kt`: `findAllByUserId`, `findByIdAndUserId`, `save`, `softDelete`, `sumValueByUserId`
4. Create `domain/port/inbound/AssetUseCase.kt`
5. Create `application/usecase/AssetUseCaseImpl.kt`
6. Create JPA entity, Spring Data repo, adapter
7. Create `AssetController.kt`, request/response DTOs
8. Write unit + integration tests

**Commit message**: `Add asset management endpoints with category filtering and soft delete`

---

### STORY-003-02: Backend — Liability CRUD endpoints

**As** an authenticated user,
**I want** API endpoints to manage my debts and outstanding balances,
**So that** the liabilities side of my net worth is always accurate.

**Acceptance Criteria:**
- [ ] Same CRUD pattern as assets
- [ ] `GET /api/v1/liabilities` returns all active liabilities sorted by `category`, then `name`
- [ ] `GET /api/v1/liabilities/total` returns `{ totalLiabilities: BigDecimal }`
- [ ] Field: `currentBalance` (remaining debt amount) — must be `>= 0`
- [ ] All endpoints scoped to authenticated user

**LiabilityCategory enum**: `CREDIT_CARD`, `PERSONAL_LOAN`, `BANK_LOAN`, `FAMILY_DEBT`, `OTHER`

**Technical Tasks:** (mirror of STORY-003-01 for Liability domain model)

**Commit message**: `Add liability management endpoints with category filtering and total calculation`

---

### STORY-003-03: Backend — Net worth snapshot creation and history

**As** an authenticated user,
**I want** to create monthly net worth snapshots and retrieve historical data,
**So that** I can track my wealth-building progress over time with exact point-in-time values.

**Acceptance Criteria:**
- [ ] `POST /api/v1/net-worth/snapshots` creates a snapshot for a given `{ snapshotDate: "2026-10-01" }`
  - Calculates `totalAssets` = sum of all currently active asset values
  - Calculates `totalLiabilities` = sum of all currently active liability values
  - Calculates `netWorth` = totalAssets − totalLiabilities
  - If a snapshot for that month already exists: updates it (idempotent)
- [ ] `GET /api/v1/net-worth/snapshots` returns all snapshots ordered by `snapshot_date ASC`
  - Optional query params: `?from=2024-01-01&to=2026-12-31`
- [ ] `GET /api/v1/net-worth/current` returns live calculation (not a stored snapshot) with current asset/liability totals
- [ ] Unit tests cover snapshot creation logic; integration test verifies persistence

**Technical Tasks:**
1. Create `domain/model/NetWorthSnapshot.kt`
2. Create `domain/port/outbound/NetWorthSnapshotRepository.kt`
3. Create `domain/port/inbound/NetWorthUseCase.kt`
4. Create `application/usecase/NetWorthUseCaseImpl.kt` with `createOrUpdateSnapshot` and `getCurrentNetWorth`
5. Create JPA entity, repo, adapter
6. Create `NetWorthController.kt` with `SnapshotRequest`, `SnapshotResponse`, `CurrentNetWorthResponse`
7. Write tests

**Commit message**: `Implement net worth snapshot creation and historical tracking endpoints`

---

### STORY-003-04: Frontend — Assets management screen

**As** the user,
**I want** a screen where I can view and manage all my assets,
**So that** I can keep my asset list current with accurate values.

**Acceptance Criteria:**
- [ ] Route: `/net-worth/assets`
- [ ] Shows a list of all active assets grouped by category
- [ ] Each item shows: name, category chip, value (formatted COP)
- [ ] Category totals shown as subheaders
- [ ] Total assets value shown at the top as a `StatCard`
- [ ] FAB (Floating Action Button) to add a new asset
- [ ] Tap any asset to open an edit bottom sheet / dialog
- [ ] Swipe-to-delete or delete icon with confirmation dialog
- [ ] Empty state when no assets exist
- [ ] Loading skeleton while fetching
- [ ] Responsive: list on mobile, 2-column grid on tablet+

**Technical Tasks:**
1. Create `features/net_worth/domain/entity/asset_entity.dart` (Freezed)
2. Create `features/net_worth/domain/repository/asset_repository.dart`
3. Create use cases: `GetAssets`, `CreateAsset`, `UpdateAsset`, `DeleteAsset`
4. Create `features/net_worth/data/` layer (datasource, model, repository impl)
5. Create `features/net_worth/presentation/bloc/asset_bloc.dart`
6. Create `features/net_worth/presentation/screen/assets_screen.dart`
7. Create `features/net_worth/presentation/widget/asset_form_sheet.dart` (reusable for create/edit)
8. Create `features/net_worth/presentation/widget/asset_list_tile.dart`
9. Write BLoC unit tests

**Commit message**: `Add assets management screen with create, edit, and delete functionality`

---

### STORY-003-05: Frontend — Liabilities management screen

**As** the user,
**I want** a screen to manage all my debts and their current balances,
**So that** I can see the full picture of what I owe.

**Acceptance Criteria:** (mirrors assets screen with liability-specific fields)
- [ ] Route: `/net-worth/liabilities`
- [ ] Shows liabilities grouped by category
- [ ] Each item shows: name, category chip, current balance (formatted COP)
- [ ] Total liabilities shown as a `StatCard` (value in error/red color — it's debt)
- [ ] FAB to add liability; tap to edit; swipe/button to delete with confirmation
- [ ] Empty state, loading skeleton, responsive layout

**Technical Tasks:** (mirror of STORY-003-04 for liabilities)

**Commit message**: `Add liabilities management screen with full CRUD operations`

---

### STORY-003-06: Frontend — Net worth overview screen with history chart

**As** the user,
**I want** a screen showing my current net worth and its historical evolution as a chart,
**So that** I can see at a glance how my wealth has grown over the years.

**Acceptance Criteria:**
- [ ] Route: `/net-worth`
- [ ] Top section: 3 `StatCard` widgets side-by-side: Total Assets, Total Liabilities, Net Worth
  - Net Worth card: value in green if positive, red if negative
  - Shows delta vs previous snapshot (e.g., "+$2,450,000 vs last month")
- [ ] "Create Snapshot" button that triggers `POST /net-worth/snapshots` for current month
- [ ] Line chart (fl_chart) showing net worth over time:
  - X axis: month/year labels
  - Y axis: COP values (formatted in millions for readability, e.g., "173M")
  - Gradient fill under the line (green → transparent)
  - Interactive tooltip on hover/tap showing exact value and date
  - Minimum 6 months shown; scrollable if more data
- [ ] Navigation links to Assets and Liabilities sub-screens
- [ ] Responsive: stat cards stack vertically on mobile; chart is full-width

**Technical Tasks:**
1. Create `features/net_worth/domain/entity/net_worth_snapshot_entity.dart`
2. Create `GetNetWorthSnapshots`, `GetCurrentNetWorth`, `CreateSnapshot` use cases
3. Create `features/net_worth/presentation/bloc/net_worth_bloc.dart`
4. Create `features/net_worth/presentation/screen/net_worth_screen.dart`
5. Create `features/net_worth/presentation/widget/net_worth_chart.dart` using `fl_chart`
6. Create reusable `core/widgets/stat_card.dart` component
7. Write BLoC unit tests with mock data

**Commit message**: `Add net worth overview screen with historical line chart and snapshot creation`

---

## Definition of Done

- [ ] All 6 stories committed
- [ ] User can create assets, create liabilities, generate a snapshot, and see the chart
- [ ] Net worth history shows all months from the Excel (can be manually imported)
- [ ] Chart renders correctly on mobile, tablet, and desktop
- [ ] All API calls return correct data and 401 on missing token
- [ ] Empty states render cleanly when no data exists

## Notes

- The initial historical data from the Excel (October 2021 – present) is NOT automatically imported.
  The user will enter it manually. A future story could add a bulk import feature, but it is out of scope here.
- COP formatting: use `NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0)` from `intl` package
- For chart Y-axis labels over 1 million COP, display as "XXX M" to avoid clutter
