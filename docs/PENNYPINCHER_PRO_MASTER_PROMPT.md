# PennyPincher Pro — Master Specification & Agent Execution Prompt

> **Project adjustments in effect** — read `docs/DECISIONES-Y-PLAN-DE-EJECUCION.md` first. Where it
> differs from this document (no infra repo, `master` branch, local-only deployment, pattern choices),
> the decisions document wins.

> **AGENT INSTRUCTIONS**: Read this entire document before writing a single line of code.
> Every decision documented here is final. When something is not specified, apply
> the highest-quality engineering judgment consistent with the standards in Section 5.
> You will work directly on GitHub via MCP. You will NOT create local files.
> You own this implementation end-to-end. The human will review the final result only.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technical Stack — Final Decisions](#2-technical-stack--final-decisions)
3. [Repository Strategy](#3-repository-strategy)
4. [Infrastructure Strategy — Free Tier](#4-infrastructure-strategy--free-tier)
5. [Code Quality Contract](#5-code-quality-contract)
6. [Domain Model & Data Dictionary](#6-domain-model--data-dictionary)
7. [Database Schema & Migration Strategy](#7-database-schema--migration-strategy)
8. [API Contract & Design Principles](#8-api-contract--design-principles)
9. [Authentication Architecture](#9-authentication-architecture)
10. [CORS Configuration](#10-cors-configuration)
11. [Multi-Tenancy Strategy](#11-multi-tenancy-strategy)
12. [Design System & Frontend Guidelines](#12-design-system--frontend-guidelines)
13. [Epics Overview & Execution Order](#13-epics-overview--execution-order)
14. [Commit Convention](#14-commit-convention)
15. [GitHub Repository Setup Instructions](#15-github-repository-setup-instructions)
16. [Deployment Guide](#16-deployment-guide)
17. [Verification Checklist](#17-verification-checklist)

---

## 1. Project Overview

### 1.1 Product Vision

**PennyPincher Pro** is a personal finance management web application that digitalizes and enhances a monthly budgeting workflow currently managed in Excel. The application tracks net worth evolution, monthly budgets, expense breakdowns, and financial independence goals.

**Current State (Excel):** The user manually maintains 5 sheets monthly:
- `FORTUNA NETA`: Monthly net worth history (assets − liabilities) since October 2021
- `PRESUPUESTO 30`: Monthly income declaration and expense category percentages
- `DETALLES 30`: Granular breakdown of credit cards, loans, fixed expenses, variable expenses, and savings
- `Metas financieras`: Four financial independence milestone targets with projection parameters
- `Notas pendientes por hacer`: Pending tasks/notes

**Target State (App):** A responsive Flutter web application backed by a Kotlin REST API and PostgreSQL, providing the same financial tracking with charts, history, goal progress, and a clean modern UI — accessible from any device.

### 1.2 Users

- **Phase 1 (Current):** Single user — the app owner (Josimar). Personal use only.
- **Phase 2 (Future):** Multi-tenant SaaS — other users manage their own independent budgets. Architecture must support this from day one via proper data isolation, but Phase 2 features (registration flow, admin panel, billing) are explicitly OUT OF SCOPE for this implementation.

### 1.3 Non-Goals (Do Not Build)

- Automatic bank transaction import / Open Banking integration
- Mobile native apps (iOS/Android) — Flutter Web only
- Social/sharing features
- Multi-currency conversion engine (TRM is tracked manually as a reference field)
- Notification system / email reminders
- Yasmin user account (single user only in Phase 1)
- Payment processing or billing
- Admin dashboard for managing other users

### 1.4 Anti-Goals (Failure Conditions)

- Slow cold start on free tier (first load > 10 seconds is unacceptable — implement health check ping strategy)
- Data leaking between future users (multi-tenant isolation must be correct from migration V1)
- OAuth2 tokens stored insecurely (no localStorage for access tokens; use httpOnly cookies or memory)
- CORS misconfiguration that blocks the frontend
- Breaking changes to the DB schema without a new Flyway migration file

---

## 2. Technical Stack — Final Decisions

These decisions are final. Do not substitute, upgrade to beta versions, or introduce alternatives.

### 2.1 Frontend

| Concern | Decision | Version |
|---|---|---|
| Framework | Flutter Web | 3.24.x (stable) |
| State Management | flutter_bloc / BLoC pattern | 8.x |
| Navigation | go_router | 14.x |
| HTTP Client | dio | 5.x |
| Dependency Injection | get_it + injectable | latest stable |
| Charts | fl_chart | 0.68.x |
| Local token storage | flutter_secure_storage | 9.x |
| Date formatting | intl | 0.19.x |
| Form validation | reactive_forms | 17.x |
| UI Components | Material 3 (built-in) | — |
| OAuth2 PKCE | flutter_web_auth_2 | 4.x |
| Code generation | build_runner + freezed + json_serializable | latest stable |

### 2.2 Backend

| Concern | Decision | Version |
|---|---|---|
| Language | Kotlin | 1.9.x |
| Framework | Spring Boot | 3.3.x |
| Build Tool | Gradle (Kotlin DSL) | 8.x |
| ORM | Spring Data JPA + Hibernate | via Spring Boot BOM |
| DB Migrations | Flyway | 10.x (via Spring Boot) |
| Security | Spring Security 6 + OAuth2 Resource Server | via Spring Boot BOM |
| JWT | nimbus-jose-jwt (via spring-security-oauth2-jose) | via Spring Boot BOM |
| JSON | Jackson with Kotlin module | via Spring Boot BOM |
| Validation | spring-boot-starter-validation (Jakarta) | via Spring Boot BOM |
| API Docs | springdoc-openapi-starter-webmvc-ui | 2.6.x |
| Testing | JUnit 5 + MockK + Testcontainers | latest stable |
| Architecture | Hexagonal (Ports & Adapters) | — |

### 2.3 Database

| Concern | Decision |
|---|---|
| Engine | PostgreSQL 16 |
| Migration Tool | Flyway (embedded in backend) |
| Connection Pool | HikariCP (default in Spring Boot) |
| UUID generation | `gen_random_uuid()` (pgcrypto) — server-side |
| Timestamps | `TIMESTAMPTZ` for all audit fields |
| Numeric precision | `NUMERIC(18,2)` for all monetary values |

### 2.4 Infrastructure & DevOps

| Concern | Decision |
|---|---|
| Containerization | Docker (multi-stage builds) |
| Orchestration manifests | Kubernetes (for future self-hosting) |
| Local dev orchestration | Docker Compose |
| CI/CD | GitHub Actions |
| Frontend hosting | Vercel (free tier) |
| Backend hosting | Render.com (free tier — Web Service) |
| Database hosting | Neon.tech (serverless PostgreSQL — free tier) |
| Container registry | GitHub Container Registry (ghcr.io) |
| Secrets management | GitHub Secrets + Render environment variables |

### 2.5 OAuth2 Provider

**Google OAuth2** — rationale: most widely used, free, no account approval needed, Google People API provides profile picture and name. The backend validates Google-issued ID tokens as JWTs using Google's JWKS endpoint.

---

## 3. Repository Strategy

Three repositories under the user's GitHub account. The agent must create all three via GitHub MCP before writing any code.

### Repository Names

| Repo | Purpose |
|---|---|
| `pennypincher-pro-frontend` | Flutter web application |
| `pennypincher-pro-backend` | Kotlin/Spring Boot REST API + Flyway migrations |
| `pennypincher-pro-infra` | Kubernetes manifests, Docker Compose, CI/CD shared configs, documentation (epics, stories, ADRs) |

### Repository Settings (apply to all three)

- **Visibility**: Private
- **Default branch**: `main`
- **Branch protection on `main`**: require PR + at least 1 approval (configure if GitHub plan allows)
- **Topics** (labels): `pennypincher-pro`, `personal-finance`, `flutter`/`kotlin`/`infrastructure`
- **License**: MIT
- **README.md**: Create a meaningful README for each repo (not auto-generated boilerplate)
- **`.gitignore`**: Language-appropriate (.gitignore for Flutter, Kotlin/JVM, generic for infra)

### Repo 3 — `pennypincher-pro-infra` Directory Structure

```
pennypincher-pro-infra/
├── README.md
├── docs/
│   ├── adr/                          # Architecture Decision Records
│   │   └── ADR-001-tech-stack.md
│   └── epics/
│       ├── EPIC-001-foundation.md
│       ├── EPIC-002-auth.md
│       ├── EPIC-003-net-worth.md
│       ├── EPIC-004-budget.md
│       ├── EPIC-005-expense-details.md
│       ├── EPIC-006-financial-goals.md
│       ├── EPIC-007-dashboard-reports.md
│       └── EPIC-008-devops-deployment.md
├── docker/
│   └── docker-compose.yml            # Full local dev stack
├── kubernetes/
│   ├── namespace.yaml
│   ├── frontend/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── ingress.yaml
│   ├── backend/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── ingress.yaml
│   │   └── configmap.yaml
│   └── database/
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── persistentvolumeclaim.yaml
│       └── secret.yaml (template — never commit real secrets)
└── .github/
    └── workflows/
        └── validate-k8s.yaml
```

**Note on DB Migrations**: Flyway migration SQL files live inside `pennypincher-pro-backend` at
`src/main/resources/db/migration/`. They are versioned with the backend code that depends on them.
This is the correct pattern — migrations and application code must be deployed atomically.

---

## 4. Infrastructure Strategy — Free Tier

### 4.1 Production Environment

```
┌─────────────────────────────────────────────────────────────┐
│                    PRODUCTION (FREE TIER)                   │
│                                                             │
│  ┌─────────────┐    HTTPS     ┌──────────────────────────┐ │
│  │   Vercel    │──────────────▶│    Render.com            │ │
│  │ (Frontend)  │              │ (Backend - Web Service)  │ │
│  │ Flutter Web │              │ Kotlin / Spring Boot     │ │
│  │ Free Tier   │              │ Free Tier (750 hrs/mo)   │ │
│  └─────────────┘              └──────────┬───────────────┘ │
│                                          │                  │
│                                          │ SSL/TLS          │
│                                          ▼                  │
│                               ┌──────────────────────────┐ │
│                               │      Neon.tech           │ │
│                               │  PostgreSQL 16           │ │
│                               │  Serverless              │ │
│                               │  Free Tier (0.5 GB)      │ │
│                               └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Free Tier Limits & Mitigations

| Service | Free Limit | Mitigation |
|---|---|---|
| Render.com | 750 hrs/month, spins down after 15 min inactivity | Implement `/actuator/health` endpoint; add Render cron job that pings it every 14 min |
| Neon.tech | 0.5 GB storage, scales to 0 when idle | Connection pooling with HikariCP (max 5 connections for free tier); use `?sslmode=require` |
| Vercel | 100 GB bandwidth/month | Flutter web tree-shaken, gzip-compressed; images are minimal in fintech UI |

### 4.3 Local Development Environment

Docker Compose (`pennypincher-pro-infra/docker/docker-compose.yml`) provides:
- PostgreSQL 16 container (port 5432)
- Backend Spring Boot container (port 8080)
- Frontend Flutter web dev server (port 3000, hot reload via `flutter run -d web-server`)
- Adminer (port 8090) — lightweight DB admin UI for dev only

### 4.4 Kubernetes Manifests (Future Self-Hosting)

K8s manifests target **Oracle Cloud Always Free** (2 ARM Ampere A1 VMs, 4 OCPU, 24 GB RAM total — sufficient for a lightweight personal app) or any CNCF-conformant cluster. Use:
- `k3s` compatible manifests
- Resource requests: `cpu: 100m, memory: 256Mi` per pod (frugal for ARM)
- Horizontal Pod Autoscaler disabled (personal use)
- Single replica per service
- `ClusterIssuer` via cert-manager for TLS (Let's Encrypt)
- Ingress via `nginx` ingress controller

---

## 5. Code Quality Contract

This section defines non-negotiable engineering standards. Every file in every repo must comply.

### 5.1 SOLID Principles

| Principle | Enforcement |
|---|---|
| **S** — Single Responsibility | Each class/function has one reason to change. Controllers do not contain business logic. Use cases do not touch HTTP. |
| **O** — Open/Closed | Domain services depend on interfaces (ports), not implementations. New income types extend the model without modifying existing code. |
| **L** — Liskov Substitution | Repository interfaces are fully substitutable between JPA and in-memory test implementations. |
| **I** — Interface Segregation | Ports are narrow. `BudgetReader` and `BudgetWriter` are separate interfaces even if one class implements both. |
| **D** — Dependency Inversion | Application layer depends on domain ports. Infrastructure layer implements those ports. Spring wires everything via constructor injection only. |

### 5.2 Clean Architecture (Backend)

```
com.pennypincher.pro
├── domain/
│   ├── model/           # Pure Kotlin data classes — no JPA, no Spring
│   ├── port/
│   │   ├── inbound/     # Use case interfaces (driving ports)
│   │   └── outbound/    # Repository/external interfaces (driven ports)
│   └── exception/       # Domain exceptions (BusinessException hierarchy)
├── application/
│   ├── usecase/         # Implements inbound ports; orchestrates domain
│   └── dto/             # Input/Output DTOs for use cases (NOT for HTTP)
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/      # JPA @Entity classes
│   │   ├── repository/  # Spring Data JPA interfaces
│   │   └── adapter/     # Implements outbound ports using JPA repos
│   ├── security/
│   │   ├── config/      # SecurityConfig, CorsConfig
│   │   ├── jwt/         # JWT validator, token service
│   │   └── filter/      # Security filters
│   ├── web/
│   │   ├── controller/  # @RestController — delegates to use cases only
│   │   ├── dto/         # HTTP Request/Response DTOs (separate from app DTOs)
│   │   ├── mapper/      # Web DTO ↔ Application DTO mappers
│   │   └── advice/      # @ControllerAdvice for exception handling
│   └── config/          # Spring beans, Flyway config, CORS config
└── PennyPincherProApplication.kt
```

**Hard rules:**
- `domain` package has zero Spring/JPA/Jakarta imports
- `application` package has zero Spring/JPA/Jakarta imports (only domain + Java stdlib)
- `infrastructure` has zero references to other `infrastructure` sub-packages (only to `application` and `domain`)
- No `@Autowired` field injection — constructor injection only
- No `lateinit var` for injected dependencies

### 5.3 Clean Architecture (Frontend — Flutter)

```
lib/
├── core/
│   ├── config/            # AppConfig, environment variables
│   ├── di/                # GetIt service locator, injectable setup
│   ├── error/             # Failure sealed classes, AppException
│   ├── network/           # DioClient, interceptors (auth, logging)
│   ├── theme/             # AppTheme, AppColors, AppTypography, AppSpacing
│   ├── router/            # AppRouter (go_router configuration)
│   └── utils/             # CurrencyFormatter, DateFormatter, validators
├── features/
│   ├── auth/
│   │   ├── data/
│   │   │   ├── datasource/    # AuthRemoteDataSource
│   │   │   ├── model/         # AuthModel (JSON)
│   │   │   └── repository/    # AuthRepositoryImpl
│   │   ├── domain/
│   │   │   ├── entity/        # User entity
│   │   │   ├── repository/    # AuthRepository interface
│   │   │   └── usecase/       # SignInWithGoogle, SignOut, GetCurrentUser
│   │   └── presentation/
│   │       ├── bloc/          # AuthBloc, AuthEvent, AuthState
│   │       ├── screen/        # LoginScreen, SplashScreen
│   │       └── widget/        # GoogleSignInButton
│   ├── dashboard/
│   ├── net_worth/
│   ├── budget/
│   ├── expense_details/
│   └── financial_goals/
└── main.dart
```

**Hard rules:**
- `domain` layer has zero Flutter/dio/get_it imports — pure Dart
- Each feature is self-contained (no feature imports from another feature directly)
- BLoC events are sealed classes (exhaustive when-clauses)
- No `BuildContext` passed to BLoC or use cases
- Use `Either<Failure, T>` (dartz package) for all domain use case returns
- Every `Bloc` has a corresponding unit test file

### 5.4 Design Patterns in Use

| Pattern | Location | Purpose |
|---|---|---|
| Repository | Both | Abstracts data persistence from business logic |
| Use Case / Interactor | Both | Encapsulates one application operation |
| Factory | Backend: mapper classes | Converts between layer DTOs |
| Strategy | Backend: expense calculation | Different calculation strategies per expense type |
| Observer (BLoC) | Frontend | Reactive UI driven by state streams |
| Adapter | Backend: infra adapters | Makes JPA repos conform to domain ports |
| Singleton | Frontend: GetIt DI | Single instances of services, repos |
| Builder | Backend: domain model builders | Construct complex domain objects safely |
| Decorator | Backend: JWT filter | Adds auth behavior to request pipeline |

### 5.5 Testing Standards

- **Backend**: Minimum 80% line coverage on `domain` and `application` packages
  - Unit tests: MockK for all dependencies; no Spring context loading for domain/application
  - Integration tests: Testcontainers (PostgreSQL) for repository adapters and full API tests
  - Test naming: `should_<expected>_when_<condition>()` in snake_case
- **Frontend**: Unit tests for all BLoC classes; widget tests for critical screens
  - Use `bloc_test` package for BLoC testing
  - Mock HTTP calls with `MockWebServer` or `dio_smart_retry` mock

### 5.6 Code Style

- **Kotlin**: ktlint (Google style) — enforced in CI
- **Flutter/Dart**: `dart format` + `dart analyze --fatal-infos` — enforced in CI
- **Maximum function length**: 30 lines
- **Maximum class length**: 200 lines
- **No commented-out code**
- **No `TODO` comments left in committed code** (use GitHub Issues instead)
- **No magic numbers or strings** — all constants in dedicated `Constants.kt` / `app_constants.dart`

---

## 6. Domain Model & Data Dictionary

All monetary values are stored and transmitted in **COP (Colombian Peso)** as `NUMERIC(18,2)`.
The TRM (exchange rate) is stored as a reference value on the monthly budget record.

### 6.1 Core Entities

#### User
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, server-generated |
| email | String | Unique, from Google profile |
| name | String | From Google profile |
| pictureUrl | String? | Google profile picture URL |
| googleSub | String | Google subject ID, unique |
| createdAt | OffsetDateTime | Auto-set on insert |
| updatedAt | OffsetDateTime | Auto-set on update |

#### Asset
Represents something the user owns and is willing to sell (liquid or semi-liquid).
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Carro Mazda", "Apto Madera" |
| value | BigDecimal | Current estimated market value in COP |
| category | AssetCategory | REAL_ESTATE, VEHICLE, BUSINESS, CASH, SAVINGS, EQUIPMENT, OTHER |
| isActive | Boolean | Soft delete |
| createdAt | OffsetDateTime | — |
| updatedAt | OffsetDateTime | — |

#### Liability
Represents a debt the user owes.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Tarjeta Nu Bank", "Prestamo Davivienda 7141" |
| currentBalance | BigDecimal | Outstanding balance in COP |
| category | LiabilityCategory | CREDIT_CARD, PERSONAL_LOAN, BANK_LOAN, FAMILY_DEBT, OTHER |
| isActive | Boolean | Soft delete |
| createdAt | OffsetDateTime | — |
| updatedAt | OffsetDateTime | — |

#### NetWorthSnapshot
Monthly point-in-time snapshot of financial position.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| snapshotDate | LocalDate | First day of month |
| totalAssets | BigDecimal | Sum of all active assets at snapshot time |
| totalLiabilities | BigDecimal | Sum of all active liabilities at snapshot time |
| netWorth | BigDecimal | totalAssets − totalLiabilities |
| createdAt | OffsetDateTime | — |

#### IncomeSource
A recurring income stream.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Ingresos AvenueCode", "Arriendo Madera" |
| category | IncomeCategory | EMPLOYMENT, RENTAL, BUSINESS, INVESTMENT, OTHER |
| amount | BigDecimal | Monthly amount in COP |
| currency | String | "COP" default; future multi-currency hint |
| isActive | Boolean | Soft delete |

#### MonthlyBudget
The monthly financial declaration — one per user per month.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| year | Int | Calendar year |
| month | Int | 1–12 |
| exchangeRate | BigDecimal? | TRM (USD/COP) at time of record |
| workDays | Int? | Billable/work days that month |
| totalIncome | BigDecimal | Sum of all income sources for that month |
| totalExpenses | BigDecimal | Sum of all expense categories |
| monthlyCashFlow | BigDecimal | totalIncome − totalExpenses |
| creditCardPercentage | BigDecimal | % of income going to credit cards |
| loanPercentage | BigDecimal | % of income going to loans |
| expensePercentage | BigDecimal | % of income going to monthly expenses |
| savingsPercentage | BigDecimal | % of income saved |
| charityPercentage | BigDecimal | % of income donated |
| createdAt | OffsetDateTime | — |
| updatedAt | OffsetDateTime | — |

Unique constraint: `(userId, year, month)`

#### CreditCard
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Tarjeta Nu Bank" |
| monthlyPayment | BigDecimal | Current monthly minimum/chosen payment |
| totalBalance | BigDecimal | Total outstanding balance |
| isActive | Boolean | — |

#### Loan
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Prestamo Occidente 190028884" |
| monthlyPayment | BigDecimal | Fixed monthly installment |
| totalBalance | BigDecimal | Remaining principal |
| isActive | Boolean | — |

#### FixedExpense
Recurring, predictable monthly cost.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Mercado", "Colegio Thiana" |
| monthlyAmount | BigDecimal | — |
| category | ExpenseCategory | FOOD, EDUCATION, TRANSPORT, UTILITIES, INSURANCE, HOUSING, HEALTH, CHARITY, OTHER |
| isActive | Boolean | — |

#### VariableExpense
Recurring but variable monthly cost.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Ocio Josi", "Netflix" |
| monthlyAmount | BigDecimal | Estimated/budgeted amount |
| category | ExpenseCategory | LEISURE, STREAMING, HOUSEHOLD_STAFF, OTHER |
| isActive | Boolean | — |

#### SavingsAccount
Savings or investment vehicle being tracked.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Licor Importado", "Cash USD" |
| monthlyContribution | BigDecimal | Monthly savings allocation |
| currentBalance | BigDecimal | Current accumulated value |
| isActive | Boolean | — |

#### FinancialGoal
Financial independence milestone.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | FK → User |
| name | String | e.g., "Seguridad Financiera" |
| level | GoalLevel | FINANCIAL_SECURITY, FINANCIAL_INDEPENDENCE, FINANCIAL_FREEDOM, ABSOLUTE_FREEDOM |
| targetAmount | BigDecimal | Goal target in COP |
| monthlyInvestment | BigDecimal | Monthly contribution toward goal |
| expectedAnnualReturn | BigDecimal | e.g., 0.11 for 11% |
| targetYears | Int? | Years to reach goal |
| createdAt | OffsetDateTime | — |
| updatedAt | OffsetDateTime | — |

---

## 7. Database Schema & Migration Strategy

### 7.1 Migration File Convention

Files live at: `pennypincher-pro-backend/src/main/resources/db/migration/`

Naming: `V{version}__{description}.sql` (double underscore required by Flyway)

| File | Content |
|---|---|
| `V1__create_users.sql` | `users` table |
| `V2__create_assets_and_liabilities.sql` | `assets`, `liabilities` tables |
| `V3__create_net_worth_snapshots.sql` | `net_worth_snapshots` table |
| `V4__create_income_sources.sql` | `income_sources` table |
| `V5__create_monthly_budgets.sql` | `monthly_budgets` table |
| `V6__create_credit_cards_and_loans.sql` | `credit_cards`, `loans` tables |
| `V7__create_expenses.sql` | `fixed_expenses`, `variable_expenses` tables |
| `V8__create_savings_accounts.sql` | `savings_accounts` table |
| `V9__create_financial_goals.sql` | `financial_goals` table |
| `V10__create_indexes.sql` | Performance indexes on all `user_id` FKs and common query columns |

### 7.2 Sample Migration — V1 (reference for style)

```sql
-- V1__create_users.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    picture_url  TEXT,
    google_sub   VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email      UNIQUE (email),
    CONSTRAINT uq_users_google_sub UNIQUE (google_sub)
);

CREATE INDEX idx_users_email ON users (email);

COMMENT ON TABLE  users            IS 'Authenticated users via Google OAuth2';
COMMENT ON COLUMN users.google_sub IS 'Google subject identifier from ID token';
```

### 7.3 Flyway Configuration

In `application.yml`:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    out-of-order: false
```

**Never modify an existing migration file after it has been applied to any environment.**
New changes always get a new version file.

---

## 8. API Contract & Design Principles

### 8.1 Base URL

- Development: `http://localhost:8080/api/v1`
- Production: `https://pennypincher-api.onrender.com/api/v1`

### 8.2 REST Conventions

- Resource names: plural nouns (`/assets`, `/liabilities`, `/monthly-budgets`)
- IDs in path for single-resource operations: `/assets/{id}`
- Filtering via query params: `/assets?category=REAL_ESTATE&isActive=true`
- Sorting: `?sort=createdAt,desc`
- Pagination: `?page=0&size=20` — standard Spring `Pageable`
- HTTP status codes:
  - `200 OK` — successful read or update
  - `201 Created` — successful creation (include `Location` header)
  - `204 No Content` — successful delete
  - `400 Bad Request` — validation failure (body: `{ errors: [...] }`)
  - `401 Unauthorized` — missing or invalid token
  - `403 Forbidden` — valid token but insufficient scope
  - `404 Not Found` — resource not found or belongs to another user
  - `409 Conflict` — uniqueness violation (e.g., duplicate monthly budget)
  - `500 Internal Server Error` — unexpected server error (never expose stack traces)

### 8.3 Standard Response Envelope

```json
{
  "data": { ... },
  "meta": {
    "timestamp": "2026-10-21T14:30:00Z",
    "requestId": "uuid"
  }
}
```

For paginated lists:
```json
{
  "data": [ ... ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 48,
    "totalPages": 3,
    "timestamp": "2026-10-21T14:30:00Z"
  }
}
```

For errors:
```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Validation failed for 1 field(s)",
    "details": [
      { "field": "monthlyAmount", "message": "must be greater than 0" }
    ]
  },
  "meta": {
    "timestamp": "2026-10-21T14:30:00Z",
    "requestId": "uuid"
  }
}
```

### 8.4 Key API Endpoints (Full list in Epic files)

```
Authentication
  POST   /auth/google/callback       Exchange Google auth code for JWT pair
  POST   /auth/refresh               Refresh access token using refresh token
  DELETE /auth/logout                Invalidate refresh token

User
  GET    /users/me                   Get own profile
  PATCH  /users/me                   Update own profile

Net Worth
  GET    /assets                     List all assets
  POST   /assets                     Create asset
  PUT    /assets/{id}                Update asset
  DELETE /assets/{id}                Soft delete asset
  GET    /liabilities                List all liabilities
  POST   /liabilities                Create liability
  PUT    /liabilities/{id}           Update liability
  DELETE /liabilities/{id}           Soft delete liability
  GET    /net-worth/snapshots        List monthly snapshots (paginated)
  POST   /net-worth/snapshots        Create/recalculate snapshot for a month
  GET    /net-worth/current          Current net worth (live calculation)

Budget
  GET    /income-sources             List income sources
  POST   /income-sources             Create income source
  PUT    /income-sources/{id}        Update income source
  DELETE /income-sources/{id}        Soft delete
  GET    /monthly-budgets            List budgets (year filter optional)
  POST   /monthly-budgets            Create budget for a month
  PUT    /monthly-budgets/{id}       Update budget
  GET    /monthly-budgets/{year}/{month}  Get specific month

Expenses
  GET    /credit-cards               List credit cards
  POST   /credit-cards               Create
  PUT    /credit-cards/{id}          Update
  DELETE /credit-cards/{id}          Soft delete
  GET    /loans                      List loans
  POST   /loans                      Create
  PUT    /loans/{id}                 Update
  DELETE /loans/{id}                 Soft delete
  GET    /fixed-expenses             List
  POST   /fixed-expenses             Create
  PUT    /fixed-expenses/{id}        Update
  DELETE /fixed-expenses/{id}        Soft delete
  GET    /variable-expenses          List
  POST   /variable-expenses          Create
  PUT    /variable-expenses/{id}     Update
  DELETE /variable-expenses/{id}     Soft delete
  GET    /savings-accounts           List savings
  POST   /savings-accounts           Create
  PUT    /savings-accounts/{id}      Update
  DELETE /savings-accounts/{id}      Soft delete

Financial Goals
  GET    /financial-goals            List goals
  POST   /financial-goals            Create goal
  PUT    /financial-goals/{id}       Update goal
  DELETE /financial-goals/{id}       Delete goal
  GET    /financial-goals/{id}/projection  Get time-to-goal projection

Dashboard
  GET    /dashboard/summary          Full dashboard data in one call
  GET    /reports/net-worth-trend    Monthly net worth over time
  GET    /reports/monthly-comparison Compare current month to last N months
```

---

## 9. Authentication Architecture

### 9.1 Flow

```
Flutter App                    Backend                      Google
    │                             │                            │
    │  1. User clicks "Sign in    │                            │
    │     with Google"            │                            │
    │─────────────────────────────────────────────────────────▶│
    │                             │   2. Google OAuth2 PKCE    │
    │                             │      authorization URL     │
    │◀─────────────────────────────────────────────────────────│
    │  3. User authenticates      │                            │
    │     in browser popup        │                            │
    │◀────────────────────────────│ 4. Redirect with           │
    │                             │    auth code               │
    │  5. POST /auth/google/callback                           │
    │     { code, codeVerifier }  │                            │
    │────────────────────────────▶│                            │
    │                             │  6. Exchange code for      │
    │                             │     Google ID token        │
    │                             │────────────────────────────▶
    │                             │  7. Validate ID token via  │
    │                             │     Google JWKS endpoint   │
    │                             │  8. Upsert user in DB      │
    │                             │  9. Issue app JWT pair     │
    │◀────────────────────────────│                            │
    │  10. Store access token in  │                            │
    │      memory; refresh token  │                            │
    │      in httpOnly cookie     │                            │
```

### 9.2 Token Strategy

- **Access Token**: JWT, signed with RS256 (RSA key pair), expiry 15 minutes
  - Claims: `sub` (userId UUID), `email`, `iat`, `exp`, `iss` ("pennypincher-pro")
  - Stored in Flutter app memory only (NOT localStorage, NOT sessionStorage)
- **Refresh Token**: Opaque UUID stored in DB (`refresh_tokens` table), expiry 30 days
  - Sent as `httpOnly; Secure; SameSite=Strict` cookie
  - Rotated on every use (refresh token rotation)
- **RSA Key Pair**: Generated on first backend startup; stored in environment variable (PEM format)

### 9.3 Backend Security Config

```
All routes                  → authenticated (default deny)
POST /auth/**               → permitAll
GET  /actuator/health       → permitAll
GET  /swagger-ui/**         → permitAll (dev profile only)
GET  /v3/api-docs/**        → permitAll (dev profile only)
Everything else             → authenticated
```

### 9.4 User Extraction

Every secured controller receives the authenticated user via a custom `@AuthenticatedUser` resolver
that extracts the `UserPrincipal` from the `SecurityContext`. Controllers never call `SecurityContextHolder` directly.

---

## 10. CORS Configuration

```kotlin
// infrastructure/config/CorsConfig.kt
@Configuration
class CorsConfig {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf(
            "http://localhost:3000",          // Flutter dev server
            "http://localhost:8080",          // Local backend (Swagger)
            "https://pennypincher-pro.vercel.app"  // Production frontend
        )
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("Authorization", "Content-Type", "X-Request-ID")
        config.allowCredentials = true        // Required for httpOnly cookie (refresh token)
        config.maxAge = 3600L
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", config)
        return source
    }
}
```

**Critical**: `allowCredentials = true` requires `allowedOrigins` to list explicit origins (not `*`).
Update the production origin once the Vercel URL is known. Store it in an environment variable:
`CORS_ALLOWED_ORIGINS=https://pennypincher-pro.vercel.app`

---

## 11. Multi-Tenancy Strategy

**Strategy: Row-Level Isolation (Phase 1 enforcement; Phase 2 ready)**

- Every table that holds user data has a `user_id UUID NOT NULL REFERENCES users(id)` column
- Every outbound repository port method signature includes `userId: UUID`
- No repository method exists without user scoping (except `User` lookup by `googleSub`)
- At the application layer, `userId` is always sourced from the authenticated `UserPrincipal` — never from the HTTP request body
- Database-level Row Level Security (RLS) is NOT used in Phase 1 (single user), but table partitioning by `user_id` is documented as the Phase 2 migration path

**Security invariant**: A request for `/assets/{id}` first fetches the asset, then verifies `asset.userId == authenticatedUserId`. If they don't match, return `404 Not Found` (not 403 — do not reveal the resource exists).

---

## 12. Design System & Frontend Guidelines

### 12.1 Visual Identity

**Concept**: Bloomberg meets modern fintech. Dark, professional, data-dense but not cluttered.
Think of it as a personal war room for your finances.

### 12.2 Color Palette

```dart
// core/theme/app_colors.dart
class AppColors {
  // Backgrounds
  static const Color backgroundPrimary   = Color(0xFF0D1117);  // Near-black
  static const Color backgroundSecondary = Color(0xFF161B22);  // Card background
  static const Color backgroundTertiary  = Color(0xFF1C2128);  // Elevated card
  static const Color backgroundModal     = Color(0xFF21262D);  // Modal/dialog

  // Brand
  static const Color brandPrimary        = Color(0xFF2563EB);  // Electric blue
  static const Color brandPrimaryHover   = Color(0xFF1D4ED8);

  // Accent
  static const Color accentGold          = Color(0xFFF59E0B);  // Amber gold (positive, wealth)
  static const Color accentGoldMuted     = Color(0xFF92400E);

  // Semantic
  static const Color success             = Color(0xFF10B981);  // Emerald green
  static const Color successMuted        = Color(0xFF064E3B);
  static const Color error               = Color(0xFFEF4444);  // Red
  static const Color errorMuted          = Color(0xFF7F1D1D);
  static const Color warning             = Color(0xFFF59E0B);  // Amber
  static const Color warningMuted        = Color(0xFF78350F);
  static const Color neutral             = Color(0xFF6B7280);

  // Text
  static const Color textPrimary         = Color(0xFFE6EDF3);  // Near-white
  static const Color textSecondary       = Color(0xFF8B949E);  // Muted
  static const Color textTertiary        = Color(0xFF6E7681);  // Very muted
  static const Color textOnBrand         = Color(0xFFFFFFFF);

  // Borders
  static const Color borderDefault       = Color(0xFF30363D);
  static const Color borderMuted         = Color(0xFF21262D);
  static const Color borderSubtle        = Color(0xFF161B22);

  // Net worth chart gradient
  static const Color chartLinePositive   = Color(0xFF10B981);
  static const Color chartLineNegative   = Color(0xFFEF4444);
  static const Color chartGradientStart  = Color(0x3310B981); // 20% opacity
  static const Color chartGradientEnd    = Color(0x0010B981); // 0% opacity
}
```

### 12.3 Typography

Font: **Inter** (Google Fonts — clean, modern, excellent for numbers)

```
Display   36sp / SemiBold  — Page titles, large KPIs
Heading 1 28sp / SemiBold  — Section headers
Heading 2 22sp / Medium    — Card titles
Heading 3 18sp / Medium    — Subsection titles
Body L    16sp / Regular   — Primary body text
Body M    14sp / Regular   — Secondary body text
Body S    12sp / Regular   — Captions, metadata
Mono L    16sp / Mono      — Currency values, numbers (use tabular nums)
Mono M    14sp / Mono      — Smaller numbers
Mono S    12sp / Mono      — Tiny numbers
Label     11sp / SemiBold  — Tags, badges, uppercase labels
```

### 12.4 Spacing & Layout

- Base unit: **8px**
- Spacing scale: 4, 8, 12, 16, 24, 32, 48, 64
- Card border radius: **12px**
- Button border radius: **8px**
- Input border radius: **8px**
- Maximum content width: **1280px** (centered on large screens)

### 12.5 Responsive Breakpoints

```dart
class AppBreakpoints {
  static const double mobile  = 0;    // < 600px
  static const double tablet  = 600;  // 600px – 1024px
  static const double desktop = 1024; // > 1024px
}
```

- **Mobile**: Bottom navigation bar, single-column layout, stacked cards
- **Tablet**: Side navigation rail (collapsed icons), 2-column grid for cards
- **Desktop**: Side navigation drawer (expanded with labels), 3-column grid, charts side-by-side

### 12.6 Navigation Structure

```
PennyPincher Pro
├── Dashboard           /dashboard
├── Net Worth           /net-worth
│   ├── Assets          /net-worth/assets
│   └── Liabilities     /net-worth/liabilities
├── Budget              /budget
│   ├── Overview        /budget/overview
│   └── Income Sources  /budget/income
├── Expenses            /expenses
│   ├── Credit Cards    /expenses/credit-cards
│   ├── Loans           /expenses/loans
│   ├── Fixed           /expenses/fixed
│   ├── Variable        /expenses/variable
│   └── Savings         /expenses/savings
├── Goals               /goals
└── Settings            /settings
    └── Profile         /settings/profile
```

### 12.7 Key UI Components to Build

- `StatCard` — KPI card with value, label, delta indicator, optional sparkline
- `CurrencyText` — formatted COP value with proper thousand separators
- `NetWorthChart` — line chart showing monthly net worth history (fl_chart)
- `ProgressBar` — labeled progress bar for goal completion
- `DataTable` — sortable, paginated table for expense lists
- `FormField` — consistent input with label, hint, error state
- `ConfirmationDialog` — reusable delete/action confirmation modal
- `EmptyState` — illustrated empty state for lists
- `LoadingOverlay` — full-screen or section loading indicator
- `AppSnackbar` — success/error/info snack bar

---

## 13. Epics Overview & Execution Order

Execute epics in strict order. Do not start Epic N+1 until Epic N is complete and all its stories are committed.

| Order | Epic ID | Name | Repos Affected |
|---|---|---|---|
| 1 | EPIC-001 | Foundation & Infrastructure | All 3 |
| 2 | EPIC-002 | Authentication & User Management | Backend, Frontend |
| 3 | EPIC-003 | Net Worth Module | Backend, Frontend |
| 4 | EPIC-004 | Budget Module | Backend, Frontend |
| 5 | EPIC-005 | Expense Details Module | Backend, Frontend |
| 6 | EPIC-006 | Financial Goals Module | Backend, Frontend |
| 7 | EPIC-007 | Dashboard & Reports | Backend, Frontend |
| 8 | EPIC-008 | DevOps & Deployment | Infra, All |

Epic detail files are in `pennypincher-pro-infra/docs/epics/`. Each file contains the full story breakdown with acceptance criteria, technical tasks, and definition of done. **Create those files in GitHub before starting any implementation.**

---

## 14. Commit Convention

### 14.1 Rules

- Language: **English only**
- Voice: **First person singular, present tense** ("Add", "Fix", "Implement", "Refactor")
- Tone: **Human, professional** — sounds like a senior engineer narrating their work
- **Never** mention AI, Claude, automated generation, or any tool name
- **Never** use Conventional Commits format (`feat:`, `fix:`, `chore:`) — use plain sentences
- **Never** say "as requested", "as per the spec", or reference this document
- Subject line: 50 characters max, no period at end
- Body (optional): explain the WHY when the what is non-obvious, wrap at 72 chars

### 14.2 Good Examples

```
Add JWT authentication filter with RS256 signature validation

Extract user ID from JWT claims on every secured request rather than
querying the database each time — keeps auth overhead under 1ms.
```

```
Implement net worth snapshot with monthly rollup calculation
```

```
Wire up Google OAuth2 PKCE flow with token rotation on refresh
```

```
Create responsive dashboard layout for mobile and desktop viewports
```

```
Add Flyway migration for assets and liabilities tables
```

### 14.3 Bad Examples (Never Do These)

```
feat: add authentication [AI generated]        ← conventional commits + AI mention
Implement authentication as requested by user  ← "as requested"
Add feature for net worth tracking             ← too vague
Fix bug                                        ← zero information
```

### 14.4 One Commit Per Story

Each user story gets its own commit on its own branch, merged to `main` via pull request if GitHub plan allows, otherwise commit directly to `main` with a clear message. Branch naming: `story/{EPIC-ID}-{STORY-ID}-{short-slug}` (e.g., `story/E002-S001-google-oauth2-backend`).

---

## 15. GitHub Repository Setup Instructions

### 15.1 Agent Actions (via GitHub MCP)

1. **Create the three repositories** with the names and settings defined in Section 3
2. **In `pennypincher-pro-infra`**: Create the full directory structure and commit this master document plus all epic files
3. **In `pennypincher-pro-backend`**: Initialize Spring Boot project with Gradle Kotlin DSL, all dependencies declared, and the Flyway migration files for the complete schema
4. **In `pennypincher-pro-frontend`**: Initialize Flutter project with clean architecture directory structure and all pubspec.yaml dependencies declared

### 15.2 Environment Variables Reference

The agent must document these variables but must NOT commit real values. Use placeholder `<VALUE>` in all committed files.

**Backend (`pennypincher-pro-backend`)**:
```
DATABASE_URL=jdbc:postgresql://<neon-host>/<db-name>?sslmode=require
DATABASE_USERNAME=<neon-user>
DATABASE_PASSWORD=<neon-password>
GOOGLE_CLIENT_ID=<google-oauth2-client-id>
GOOGLE_CLIENT_SECRET=<google-oauth2-client-secret>
JWT_PRIVATE_KEY_PEM=<rsa-private-key-pem-base64>
JWT_PUBLIC_KEY_PEM=<rsa-public-key-pem-base64>
CORS_ALLOWED_ORIGINS=https://pennypincher-pro.vercel.app
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

**Frontend (`pennypincher-pro-frontend`)**:
```
FLUTTER_API_BASE_URL=https://pennypincher-api.onrender.com/api/v1
FLUTTER_GOOGLE_CLIENT_ID=<google-oauth2-client-id>
```

---

## 16. Deployment Guide

### 16.1 Step 1 — Set Up Neon PostgreSQL

1. Go to [neon.tech](https://neon.tech), sign up, create a project named `pennypincher-pro`
2. Create a database named `pennypincher`
3. Copy the connection string — it includes SSL mode
4. Flyway will run migrations automatically on first backend startup

### 16.2 Step 2 — Set Up Google OAuth2

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a project `PennyPincher Pro`
3. Enable Google+ API (for profile info)
4. Create OAuth2 credentials → Web application
5. Authorized JavaScript origins: `https://pennypincher-pro.vercel.app`, `http://localhost:3000`
6. Authorized redirect URIs: `https://pennypincher-api.onrender.com/api/v1/auth/google/callback`, `http://localhost:8080/api/v1/auth/google/callback`
7. Copy Client ID and Client Secret

### 16.3 Step 3 — Generate RSA Key Pair

```bash
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
# Base64-encode for environment variable storage:
base64 -i private.pem | tr -d '\n'   → JWT_PRIVATE_KEY_PEM
base64 -i public.pem  | tr -d '\n'   → JWT_PUBLIC_KEY_PEM
```

### 16.4 Step 4 — Deploy Backend to Render.com

1. Sign up at [render.com](https://render.com)
2. New → Web Service → Connect GitHub → `pennypincher-pro-backend`
3. Runtime: Docker
4. Set all environment variables from Section 15.2
5. Health check path: `/actuator/health`
6. Note the service URL (e.g., `https://pennypincher-pro-backend.onrender.com`)
7. Add a Render Cron Job: `curl -s https://pennypincher-pro-backend.onrender.com/actuator/health` every 14 minutes to prevent spin-down

### 16.5 Step 5 — Deploy Frontend to Vercel

1. Sign up at [vercel.com](https://vercel.com)
2. New Project → Import `pennypincher-pro-frontend`
3. Framework Preset: Other
4. Build Command: `flutter build web --release --dart-define=API_BASE_URL=$FLUTTER_API_BASE_URL --dart-define=GOOGLE_CLIENT_ID=$FLUTTER_GOOGLE_CLIENT_ID`
5. Output Directory: `build/web`
6. Set environment variables
7. Note the deployment URL → update `CORS_ALLOWED_ORIGINS` in backend

### 16.6 Local Development

```bash
# Clone all three repos into the same parent directory
git clone https://github.com/<user>/pennypincher-pro-infra
git clone https://github.com/<user>/pennypincher-pro-backend
git clone https://github.com/<user>/pennypincher-pro-frontend

# Start the full local stack
cd pennypincher-pro-infra
docker-compose -f docker/docker-compose.yml up -d

# Backend starts with Flyway auto-migration
# Frontend: in pennypincher-pro-frontend:
flutter run -d web-server --web-port=3000
```

---

## 17. Verification Checklist

The agent must verify each item before declaring implementation complete.

### Backend
- [ ] All 9 Flyway migrations apply cleanly on a fresh database with `flyway:migrate`
- [ ] `./gradlew test` passes with >= 80% coverage on `domain` and `application` packages
- [ ] `./gradlew ktlintCheck` passes with zero violations
- [ ] Swagger UI at `/swagger-ui.html` shows all endpoints correctly documented
- [ ] `POST /auth/google/callback` returns access token + sets httpOnly refresh cookie
- [ ] `POST /auth/refresh` rotates the refresh token and returns a new access token
- [ ] A request without a token to any secured endpoint returns `401`
- [ ] A request with a valid token for User A cannot read User B's data (returns 404)
- [ ] CORS: preflight `OPTIONS` request from the frontend origin returns `200` with correct headers
- [ ] `/actuator/health` returns `{"status":"UP"}` without authentication
- [ ] Docker image builds successfully: `docker build -t pennypincher-backend .`

### Frontend
- [ ] `flutter analyze` returns zero errors and zero warnings
- [ ] `flutter test` passes all unit and widget tests
- [ ] `flutter build web --release` succeeds without errors
- [ ] Login screen renders on mobile (375px), tablet (768px), and desktop (1280px)
- [ ] Dashboard loads and displays data after authentication
- [ ] All CRUD operations (create, read, update, delete) work for each entity
- [ ] Net worth chart renders with correct historical data
- [ ] Financial goals show correct progress percentages
- [ ] Navigation works correctly across all breakpoints
- [ ] App handles `401` response by redirecting to login

### Infrastructure
- [ ] `docker-compose up` starts all services and backend connects to PostgreSQL
- [ ] All Kubernetes manifests validate: `kubectl apply --dry-run=client -f kubernetes/`
- [ ] GitHub Actions CI pipeline runs successfully on push to `main`

---

*This document was authored as the complete source of truth for PennyPincher Pro.
All implementation decisions flow from here. If a conflict exists between this document
and an epic file, this document takes precedence.*
