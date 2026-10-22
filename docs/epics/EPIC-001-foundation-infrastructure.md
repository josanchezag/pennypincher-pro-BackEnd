# EPIC-001 — Foundation & Infrastructure

**Epic ID**: EPIC-001
**Priority**: P0 — Must complete before any other epic
**Repos**: `pennypincher-pro-frontend`, `pennypincher-pro-backend`, `pennypincher-pro-infra`

> **Project adjustment (see `docs/DECISIONES-Y-PLAN-DE-EJECUCION.md`)**: there is no `pennypincher-pro-infra`
> repository. Everything that would live there (docs, ADRs, docker-compose, Kubernetes manifests, k8s
> validation workflow) lives in `pennypincher-pro-backend` under `docs/` and `infra/`. Default branch is `master`.

## Business Value

Establishes the scaffolding all subsequent work builds on. Without a correctly structured codebase,
CI/CD pipeline, and local dev environment, every other epic is at risk of requiring rework.

## Acceptance Criteria (Epic Level)

- All three GitHub repositories exist with correct structure and branch protection
- A developer can clone the repos, run `docker-compose up`, and have a fully working local environment in under 10 minutes
- Both backend and frontend compile and pass linting on CI without manual intervention
- Kubernetes manifests validate against a dry-run without errors
- The epic .md files for all 8 epics exist in `pennypincher-pro-infra/docs/epics/`

---

## Stories

### STORY-001-01: Create and scaffold three GitHub repositories

**As** the application owner,
**I want** three properly structured GitHub repositories,
**So that** all project code has a well-organized home from day one.

**Acceptance Criteria:**
- [ ] Repos created: `pennypincher-pro-frontend`, `pennypincher-pro-backend`, `pennypincher-pro-infra`
- [ ] All repos are private
- [ ] Default branch is `main`
- [ ] Each repo has a meaningful `README.md` explaining the repo's role
- [ ] Each repo has a language-appropriate `.gitignore`
- [ ] `pennypincher-pro-infra` contains the full directory structure defined in the master spec
- [ ] All 8 epic `.md` files are committed to `pennypincher-pro-infra/docs/epics/`
- [ ] Master spec `PENNYPINCHER_PRO_MASTER_PROMPT.md` is committed to `pennypincher-pro-infra/docs/`

**Technical Tasks:**
1. Create repositories via GitHub MCP
2. Initialize `pennypincher-pro-infra` with directory structure
3. Commit master spec and epic files to infra repo
4. Create `.gitignore` for each repo (Flutter/Dart for frontend; Gradle/JVM for backend; generic for infra)

**Commit message**: `Initialize repository structure with project documentation`

---

### STORY-001-02: Scaffold Kotlin/Spring Boot backend project

**As** a developer,
**I want** a properly structured Spring Boot project with all dependencies configured,
**So that** I can start implementing features without spending time on boilerplate setup.

**Acceptance Criteria:**
- [ ] Gradle Kotlin DSL `build.gradle.kts` with all dependencies from master spec Section 2.2
- [ ] `application.yml` configured for three profiles: `default` (local), `test`, `prod`
- [ ] `application-prod.yml` reads all sensitive values from environment variables
- [ ] Clean Architecture package structure created (empty packages with `.gitkeep`)
- [ ] `PennyPincherProApplication.kt` with `@SpringBootApplication` working
- [ ] `/actuator/health` endpoint returns `{"status":"UP"}`
- [ ] `./gradlew build` succeeds with zero errors
- [ ] `./gradlew ktlintCheck` passes (ktlint configured in `build.gradle.kts`)

**Technical Tasks:**
1. Create `build.gradle.kts` with:
   - `org.springframework.boot` plugin 3.3.x
   - `kotlin("jvm")` 1.9.x
   - `kotlin("plugin.spring")`
   - `kotlin("plugin.jpa")`
   - `id("org.jlleitschuh.gradle.ktlint")`
   - All dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa,
     spring-boot-starter-security, spring-boot-starter-oauth2-resource-server,
     spring-boot-starter-validation, spring-boot-starter-actuator,
     spring-boot-starter-test, postgresql, flyway-core, flyway-database-postgresql,
     springdoc-openapi-starter-webmvc-ui, jackson-module-kotlin, mockk, testcontainers
2. Create `settings.gradle.kts` with project name `pennypincher-pro-backend`
3. Create `src/main/kotlin/com/pennypincher/pro/` package tree
4. Create `src/main/resources/application.yml` with all profiles
5. Create `src/main/resources/db/migration/` directory with `.gitkeep`
6. Configure ktlint with `.editorconfig`

**Commit message**: `Scaffold Spring Boot project with clean architecture package structure`

---

### STORY-001-03: Scaffold Flutter frontend project

**As** a developer,
**I want** a properly structured Flutter project with all dependencies and the design system scaffolded,
**So that** I can implement features on top of a consistent foundation.

**Acceptance Criteria:**
- [ ] `pubspec.yaml` with all dependencies from master spec Section 2.1
- [ ] Clean Architecture feature-based directory structure created
- [ ] `AppTheme`, `AppColors`, `AppTypography`, `AppSpacing` classes implemented per design system (Section 12)
- [ ] `AppRouter` with go_router configured (placeholder routes for all screens)
- [ ] GetIt service locator configured with `injectable`
- [ ] `flutter analyze` returns zero errors
- [ ] `flutter build web --release` succeeds
- [ ] App runs and shows a loading/placeholder screen

**Technical Tasks:**
1. Initialize Flutter project
2. Create `pubspec.yaml` with all dependencies
3. Create directory structure per master spec Section 5.3
4. Implement `core/theme/` files (AppColors, AppTypography, AppSpacing, AppTheme)
5. Set up GetIt DI in `core/di/injection.dart`
6. Set up AppRouter with all named routes as stubs
7. Create `main.dart` that initializes DI, theme, and router
8. Configure `flutter_lints` with `analysis_options.yaml`
9. Add Inter font from Google Fonts to `pubspec.yaml`
10. Configure `web/index.html` with correct meta tags, title "PennyPincher Pro"

**Commit message**: `Scaffold Flutter project with design system and routing infrastructure`

---

### STORY-001-04: Configure Flyway database migrations (full schema)

**As** the system,
**I want** all database tables created automatically via Flyway on backend startup,
**So that** the schema is always in sync with the application code.

**Acceptance Criteria:**
- [ ] All migration files V1–V10 exist in `src/main/resources/db/migration/`
- [ ] Migrations apply cleanly on a fresh PostgreSQL database
- [ ] All tables have correct columns, types, constraints, and indexes per Section 6 and 7
- [ ] All monetary columns use `NUMERIC(18,2)`
- [ ] All UUID primary keys use `gen_random_uuid()` default
- [ ] All audit timestamps use `TIMESTAMPTZ`
- [ ] All `user_id` foreign key columns have indexes
- [ ] `flyway_schema_history` table exists after migration

**Technical Tasks:**
1. Create `V1__create_users.sql`
2. Create `V2__create_assets_and_liabilities.sql`
3. Create `V3__create_net_worth_snapshots.sql`
4. Create `V4__create_income_sources.sql`
5. Create `V5__create_monthly_budgets.sql`
6. Create `V6__create_credit_cards_and_loans.sql`
7. Create `V7__create_expenses.sql`
8. Create `V8__create_savings_accounts.sql`
9. Create `V9__create_financial_goals.sql`
10. Create `V10__create_indexes.sql`
11. Write integration test that starts Testcontainers PostgreSQL and verifies all migrations apply

**Commit message**: `Add complete database schema via Flyway migrations V1 through V10`

---

### STORY-001-05: Create Docker files for all services

**As** a developer,
**I want** Dockerfiles for both backend and frontend,
**So that** I can build reproducible container images for any environment.

**Acceptance Criteria:**
- [ ] `pennypincher-pro-backend/Dockerfile` builds a production image with multi-stage build
- [ ] `pennypincher-pro-frontend/Dockerfile` builds a production image serving Flutter web via Nginx
- [ ] Backend image: uses `eclipse-temurin:21-jre-alpine` as final stage (minimal size)
- [ ] Frontend image: uses `nginx:alpine` as final stage; Flutter web assets served correctly
- [ ] `docker build` succeeds for both repos
- [ ] Backend container starts and `/actuator/health` responds when environment variables are set

**Backend Dockerfile structure:**
```dockerfile
# Stage 1: Build
FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

**Frontend Dockerfile structure:**
```dockerfile
# Stage 1: Build Flutter web
FROM ghcr.io/cirruslabs/flutter:stable AS builder
WORKDIR /app
COPY pubspec.* ./
RUN flutter pub get
COPY . .
RUN flutter build web --release

# Stage 2: Serve with Nginx
FROM nginx:alpine
COPY --from=builder /app/build/web /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

**Commit message**: `Add multi-stage Dockerfiles for backend and frontend services`

---

### STORY-001-06: Create Docker Compose for local development

**As** a developer,
**I want** a single `docker-compose.yml` that starts the full local stack,
**So that** I can develop and test locally without installing PostgreSQL or managing processes manually.

**Acceptance Criteria:**
- [ ] `docker-compose.yml` in `pennypincher-pro-infra/docker/`
- [ ] Services: `postgres`, `backend`, `adminer`
- [ ] PostgreSQL 16 with health check
- [ ] Backend depends on postgres health check passing
- [ ] Adminer available at `localhost:8090`
- [ ] Backend available at `localhost:8080`
- [ ] Volume for PostgreSQL data persistence between restarts
- [ ] `.env.local.example` file documenting all required environment variables
- [ ] `README.md` in `docker/` with quickstart instructions

**Commit message**: `Add Docker Compose configuration for local development environment`

---

### STORY-001-07: Create Kubernetes manifests

**As** the infrastructure owner,
**I want** Kubernetes manifests for all services,
**So that** the application can be self-hosted on any CNCF-conformant cluster in the future.

**Acceptance Criteria:**
- [ ] `namespace.yaml` creates `pennypincher-pro` namespace
- [ ] Backend: `deployment.yaml`, `service.yaml`, `ingress.yaml`, `configmap.yaml`
- [ ] Frontend: `deployment.yaml`, `service.yaml`, `ingress.yaml`
- [ ] Database: `deployment.yaml`, `service.yaml`, `persistentvolumeclaim.yaml`, `secret.yaml` (template)
- [ ] All resource requests and limits defined (`cpu: 100m/500m`, `memory: 256Mi/512Mi`)
- [ ] Liveness and readiness probes configured (backend: `/actuator/health/liveness`, `/actuator/health/readiness`)
- [ ] `kubectl apply --dry-run=client -f kubernetes/` succeeds
- [ ] Ingress uses `nginx` ingress class with TLS (cert-manager annotation)

**Commit message**: `Add Kubernetes manifests for all services targeting k3s-compatible clusters`

---

### STORY-001-08: Configure GitHub Actions CI pipelines

**As** the development team,
**I want** CI pipelines that run on every push,
**So that** code quality and compilation are always verified automatically.

**Acceptance Criteria:**
- [ ] `pennypincher-pro-backend/.github/workflows/ci.yml`:
  - Runs on push to `main` and on PRs
  - Steps: checkout, setup JDK 21, Gradle cache, `ktlintCheck`, `test`, `bootJar`
  - Uploads test results as artifact
- [ ] `pennypincher-pro-frontend/.github/workflows/ci.yml`:
  - Runs on push to `main` and on PRs
  - Steps: checkout, setup Flutter stable, `flutter pub get`, `flutter analyze`, `flutter test`, `flutter build web --release`
- [ ] `pennypincher-pro-infra/.github/workflows/validate-k8s.yml`:
  - Validates all K8s manifests with `kubectl apply --dry-run=client`
- [ ] All pipelines pass on first run

**Commit message**: `Configure GitHub Actions CI pipelines for all three repositories`

---

## Definition of Done

- [ ] All 8 stories completed and committed
- [ ] `docker-compose up` brings up full local stack successfully
- [ ] Backend CI pipeline is green
- [ ] Frontend CI pipeline is green
- [ ] All K8s manifests validate
- [ ] A second developer (or the agent itself) could clone all three repos and have them running locally in under 10 minutes by following the README

## Notes

- The RSA key pair for JWT does NOT need to be generated at this stage — a placeholder config is sufficient
- The Google OAuth2 credentials are not needed until EPIC-002
- Adminer is a dev-only tool; it must NOT be included in the production K8s manifests
