# EPIC-008 — DevOps & Deployment

**Epic ID**: EPIC-008
**Priority**: P0 for CI; P1 for deployment
**Repos**: All three
**Depends on**: All previous epics complete (deploy after full implementation)

> **Project adjustment**: for now only the local parts are implemented (CI workflows, Dockerfiles,
> docker-compose, Kubernetes manifests validated by dry-run, `render.yaml`/`vercel.json` as files).
> Nothing is actually deployed to Render, Vercel, or Neon yet. See `docs/DECISIONES-Y-PLAN-DE-EJECUCION.md`.

## Business Value

A working local development environment and automated CI pipeline prevent regressions and
reduce the friction of making changes. Free-tier deployment makes the app accessible from
any device, fulfilling the "web app" requirement. Without deployment, the app only exists
on a developer's machine.

## Acceptance Criteria (Epic Level)

- The full application is accessible on the internet via Vercel (frontend) and Render (backend)
- Both CI pipelines run green on every push
- Local development stack starts with a single `docker-compose up` command
- Kubernetes manifests validate and are ready for future self-hosting
- The backend does not spin down unexpectedly (keep-alive ping configured)
- Environment configuration is documented and reproducible

---

## Stories

### STORY-008-01: Complete GitHub Actions CI for backend

**As** the development process,
**I want** the backend CI to run tests and build on every push,
**So that** regressions are caught immediately and the build is always deployable.

**Acceptance Criteria:**
- [ ] `.github/workflows/ci.yml` in `pennypincher-pro-backend`
- [ ] Triggers: push to `main`, pull_request to `main`
- [ ] Jobs:
  1. `lint`: run `./gradlew ktlintCheck`
  2. `test`: run `./gradlew test` with PostgreSQL via `services.postgres` in the workflow
  3. `build`: run `./gradlew bootJar`
- [ ] Test results uploaded as artifact
- [ ] Build artifact (fat JAR) uploaded as artifact on `main` branch pushes
- [ ] Caching: Gradle dependencies cached with `actions/cache`
- [ ] Uses `eclipse-temurin:21` Java setup
- [ ] Test step uses `services:` to spin up PostgreSQL for Testcontainers (or use GH Actions service container)
- [ ] CI fails if coverage < 80% on domain + application (JaCoCo configured in Gradle)

**Technical Tasks:**
1. Create `.github/workflows/ci.yml`
2. Configure JaCoCo in `build.gradle.kts` with minimum coverage thresholds
3. Ensure all Testcontainers tests can run in CI (no Docker-in-Docker issues — use `TESTCONTAINERS_RYUK_DISABLED=true`)

**Commit message**: `Configure backend CI pipeline with linting, testing, and build verification`

---

### STORY-008-02: Complete GitHub Actions CI for frontend

**As** the development process,
**I want** the frontend CI to analyze and build Flutter web on every push,
**So that** broken builds and analysis errors are caught before they reach production.

**Acceptance Criteria:**
- [ ] `.github/workflows/ci.yml` in `pennypincher-pro-frontend`
- [ ] Triggers: push to `main`, pull_request to `main`
- [ ] Jobs:
  1. `analyze`: `flutter pub get && flutter analyze --fatal-infos`
  2. `test`: `flutter test --coverage`
  3. `build`: `flutter build web --release --dart-define=API_BASE_URL=https://placeholder.onrender.com --dart-define=GOOGLE_CLIENT_ID=placeholder`
- [ ] Uses `subosito/flutter-action@v2` with `channel: stable`
- [ ] Flutter and Dart version pinned in workflow
- [ ] Coverage report uploaded as artifact

**Technical Tasks:**
1. Create `.github/workflows/ci.yml`
2. Ensure all Flutter tests use mocked dependencies

**Commit message**: `Configure Flutter web CI pipeline with analysis, tests, and release build`

---

### STORY-008-03: Configure keep-alive for Render backend

**As** the backend service on Render free tier,
**I want** to receive periodic pings so I don't spin down,
**So that** the user doesn't experience a 30+ second cold start when they haven't used the app in 15 minutes.

**Acceptance Criteria:**
- [ ] A Render Cron Job (or GitHub Actions schedule) pings `GET /actuator/health` every 14 minutes
- [ ] The health check endpoint returns `200 { "status": "UP" }` in under 500ms
- [ ] Spring Actuator is configured with only `health` endpoint exposed publicly
- [ ] Spring Actuator has `livenessState` and `readinessState` enabled for K8s probes
- [ ] `management.endpoints.web.exposure.include=health` (not wildcard)

**Technical Tasks:**
1. Configure `application.yml`:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health
     endpoint:
       health:
         probes:
           enabled: true
         show-details: when-authorized
   ```
2. Document Render Cron Job setup in `pennypincher-pro-infra/docs/` (how to create the cron in Render dashboard)
3. Alternatively: add `.github/workflows/keep-alive.yml` that runs `curl` on a schedule:
   ```yaml
   on:
     schedule:
       - cron: '*/14 * * * *'
   ```

**Commit message**: `Configure Spring Actuator health endpoint and keep-alive strategy for free tier`

---

### STORY-008-04: Production deployment — Neon PostgreSQL setup

**As** the production environment,
**I want** the database configured on Neon.tech with correct settings,
**So that** the backend connects securely and Flyway runs migrations on startup.

**Acceptance Criteria:**
- [ ] `application-prod.yml` configures datasource from `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- [ ] SSL mode required: connection string includes `?sslmode=require`
- [ ] HikariCP configured for free tier limits:
  ```yaml
  spring:
    datasource:
      hikari:
        maximum-pool-size: 5
        minimum-idle: 1
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
  ```
- [ ] Flyway runs automatically on startup (`spring.flyway.enabled=true`)
- [ ] Backend Docker container logs confirm "Successfully applied N migrations to schema 'public'"
- [ ] README in `pennypincher-pro-backend` documents the Neon setup steps

**Technical Tasks:**
1. Finalize `application-prod.yml` with all production settings
2. Ensure Docker image passes env vars correctly
3. Write Neon setup instructions in README

**Commit message**: `Configure production database connection for Neon PostgreSQL with SSL and connection pooling`

---

### STORY-008-05: Production deployment — Render backend service

**As** the user,
**I want** the backend API running on Render.com,
**So that** the Flutter web app has a live endpoint to connect to.

**Acceptance Criteria:**
- [ ] `Dockerfile` in `pennypincher-pro-backend` works for Render deployment
- [ ] `render.yaml` (Infrastructure as Code) in `pennypincher-pro-backend` defining the web service:
  ```yaml
  services:
    - type: web
      name: pennypincher-pro-backend
      runtime: docker
      plan: free
      healthCheckPath: /actuator/health
      envVars:
        - key: SPRING_PROFILES_ACTIVE
          value: prod
        - key: DATABASE_URL
          sync: false
        - key: DATABASE_USERNAME
          sync: false
        - key: DATABASE_PASSWORD
          sync: false
        - key: GOOGLE_CLIENT_ID
          sync: false
        - key: JWT_PRIVATE_KEY_PEM
          sync: false
        - key: JWT_PUBLIC_KEY_PEM
          sync: false
        - key: CORS_ALLOWED_ORIGINS
          sync: false
  ```
- [ ] README documents all manual steps for first deployment to Render

**Commit message**: `Add Render deployment configuration and production infrastructure documentation`

---

### STORY-008-06: Production deployment — Vercel frontend

**As** the user,
**I want** the Flutter web app deployed to Vercel,
**So that** I can access PennyPincher Pro from any browser on any device.

**Acceptance Criteria:**
- [ ] `vercel.json` in `pennypincher-pro-frontend`:
  ```json
  {
    "buildCommand": "flutter build web --release --dart-define=API_BASE_URL=$FLUTTER_API_BASE_URL --dart-define=GOOGLE_CLIENT_ID=$FLUTTER_GOOGLE_CLIENT_ID",
    "outputDirectory": "build/web",
    "framework": null,
    "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]
  }
  ```
- [ ] `rewrites` rule ensures Flutter web routing (all paths serve `index.html` — SPA behavior)
- [ ] `FLUTTER_API_BASE_URL` and `FLUTTER_GOOGLE_CLIENT_ID` set in Vercel environment variables
- [ ] Frontend loads correctly at Vercel URL after deployment
- [ ] No mixed-content warnings (both frontend and backend use HTTPS in production)
- [ ] README documents Vercel deployment steps

**Commit message**: `Add Vercel deployment configuration with SPA routing and environment variable setup`

---

### STORY-008-07: Docker Compose finalization for local development

**As** a developer,
**I want** a fully polished `docker-compose.yml` for the complete local stack,
**So that** the entire application can be tested locally without any external dependencies.

**Acceptance Criteria:**
- [ ] `docker-compose.yml` in `pennypincher-pro-infra/docker/`:
  - `postgres`: PostgreSQL 16, port 5432, health check, named volume `pgdata`
  - `backend`: built from `../pennypincher-pro-backend`, port 8080, depends on postgres health, all env vars from `.env` file
  - `adminer`: port 8090, depends on postgres
- [ ] `.env.local.example` file with all required variables and comments
- [ ] `.env.local` in `.gitignore` (never commit real values)
- [ ] `docker-compose.yml` mounts source code for development (or use built image — choose built for simplicity)
- [ ] `README.md` in `docker/` with step-by-step quickstart
- [ ] `docker-compose up --build` starts all services successfully
- [ ] `docker-compose down -v` tears down cleanly including volumes

**Technical Tasks:**
1. Finalize `docker/docker-compose.yml`
2. Create `.env.local.example`
3. Create `docker/README.md`

**Commit message**: `Finalize Docker Compose local development environment with documentation`

---

### STORY-008-08: Kubernetes manifests finalization

**As** the infrastructure owner,
**I want** complete, validated Kubernetes manifests,
**So that** the application can be migrated to self-hosted K8s (Oracle Cloud Always Free) in the future.

**Acceptance Criteria:**
- [ ] All manifests in `pennypincher-pro-infra/kubernetes/`
- [ ] `namespace.yaml`: creates `pennypincher-pro` namespace
- [ ] Backend manifests:
  - `deployment.yaml`: 1 replica, image from ghcr.io, resource limits, liveness/readiness probes
  - `service.yaml`: ClusterIP, port 8080
  - `ingress.yaml`: nginx ingress class, TLS via cert-manager annotation, host `api.pennypincher.yourdomain.com`
  - `configmap.yaml`: non-secret config (`SPRING_PROFILES_ACTIVE=prod`, `SERVER_PORT=8080`)
  - `secret.yaml`: template only — placeholder values, documented instructions for real secrets
- [ ] Frontend manifests:
  - `deployment.yaml`: 1 replica, nginx image serving Flutter web
  - `service.yaml`: ClusterIP, port 80
  - `ingress.yaml`: host `pennypincher.yourdomain.com`
- [ ] Database manifests:
  - `deployment.yaml`: PostgreSQL 16
  - `persistentvolumeclaim.yaml`: 5Gi, ReadWriteOnce
  - `service.yaml`: ClusterIP, port 5432
- [ ] `kubectl apply --dry-run=client -f kubernetes/` passes with zero errors
- [ ] README in `kubernetes/` documents how to deploy to k3s on Oracle Cloud

**Technical Tasks:**
1. Write all manifest files
2. Validate with dry-run in CI (`validate-k8s.yaml` workflow)
3. Create `kubernetes/README.md` with Oracle Cloud Always Free setup guide

**Commit message**: `Complete Kubernetes manifests with resource limits and ingress configuration`

---

## Definition of Done

- [ ] All 8 stories committed
- [ ] Both CI pipelines run green on `main`
- [ ] Backend live at `https://pennypincher-pro-backend.onrender.com/actuator/health`
- [ ] Frontend live at `https://pennypincher-pro.vercel.app`
- [ ] Full sign-in flow works on production URLs (Google OAuth2 redirect URIs configured)
- [ ] Keep-alive ping prevents cold starts
- [ ] Local Docker Compose stack runs cleanly
- [ ] K8s manifests validated in CI

## Notes

- **Order of deployment operations:**
  1. Set up Neon database (get connection string)
  2. Generate RSA key pair (get PEM values)
  3. Configure Google OAuth2 (get client ID/secret, set redirect URIs)
  4. Deploy backend to Render (set env vars, confirm health check passes, get public URL)
  5. Update `CORS_ALLOWED_ORIGINS` in Render env vars once Vercel URL is known
  6. Deploy frontend to Vercel (set env vars with backend URL)
  7. Update Google Cloud Console with final Vercel redirect URI
  8. Test complete sign-in flow end-to-end

- **Render free tier cold start**: The keep-alive approach is a practical workaround.
  If the user upgrades to Render Starter ($7/month), the service never spins down —
  document this as an upgrade option in the README.

- **Neon free tier limits**: At 0.5 GB, the personal finance data will stay well within limits
  for years of use. The `pgdata` in the schema is compact (no file uploads, no images stored).

- **GitHub Container Registry**: For the K8s deployment, the backend image should be pushed
  to `ghcr.io/[github-username]/pennypincher-pro-backend:latest` via a GitHub Actions
  `publish` workflow triggered on `main` pushes. Add this workflow to the backend repo CI.
