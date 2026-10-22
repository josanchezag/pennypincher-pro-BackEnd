# EPIC-002 — Authentication & User Management

**Epic ID**: EPIC-002
**Priority**: P0 — Required before any protected feature
**Repos**: `pennypincher-pro-backend`, `pennypincher-pro-frontend`
**Depends on**: EPIC-001 complete

## Business Value

Secure, frictionless sign-in with Google ensures the user's financial data is protected and accessible
only to them. Building multi-tenant data isolation from the first user sets the foundation for future
SaaS expansion without requiring a painful migration.

## Acceptance Criteria (Epic Level)

- User can sign in with their Google account from the Flutter web app
- Backend issues short-lived JWT access tokens and long-lived httpOnly refresh tokens
- All API endpoints except `/auth/**` and `/actuator/health` require a valid JWT
- User data is scoped by `user_id` on every database query
- Token rotation works correctly — refreshing issues a new refresh token and invalidates the old one
- A returning user's profile is updated from Google on each login

---

## Stories

### STORY-002-01: Backend — Google OAuth2 token validation and user upsert

**As** the backend system,
**I want** to validate Google ID tokens and create/update user records,
**So that** every authenticated request maps to a known user in the database.

**Acceptance Criteria:**
- [ ] `POST /api/v1/auth/google/callback` accepts `{ code: string, redirectUri: string, codeVerifier: string }`
- [ ] Backend exchanges the auth code for a Google ID token using Google's token endpoint
- [ ] Backend validates the ID token: signature, issuer (`accounts.google.com`), audience (Google Client ID), expiry
- [ ] If user with `google_sub` exists: update `name`, `picture_url`, `email`
- [ ] If user does not exist: create new user record
- [ ] Returns `{ accessToken: string }` in response body
- [ ] Sets refresh token as `httpOnly; Secure; SameSite=Strict` cookie named `ppp_refresh`
- [ ] Both tokens are included in an `AuthResponseDto`
- [ ] Unit tests cover: valid token flow, expired token, wrong audience, new user creation, existing user update

**Technical Tasks:**
1. Create `domain/model/User.kt` data class
2. Create `domain/port/outbound/UserRepository.kt` interface with `findByGoogleSub`, `findById`, `save`
3. Create `domain/port/inbound/AuthUseCase.kt` interface: `authenticateWithGoogle(code, redirectUri, codeVerifier): AuthResult`
4. Create `application/usecase/AuthUseCaseImpl.kt`:
   - Calls Google token endpoint via `RestTemplate` or `WebClient`
   - Validates ID token claims
   - Upserts user via `UserRepository`
   - Delegates token generation to `JwtService`
5. Create `infrastructure/persistence/entity/UserEntity.kt` (JPA)
6. Create `infrastructure/persistence/repository/UserJpaRepository.kt` (Spring Data)
7. Create `infrastructure/persistence/adapter/UserRepositoryAdapter.kt`
8. Create `infrastructure/security/jwt/JwtService.kt`: generates access token (RS256, 15 min) and refresh token (UUID, stored in DB)
9. Create `V11__create_refresh_tokens.sql` migration: `refresh_tokens(id, user_id, token_hash, expires_at, revoked)`
10. Create `infrastructure/web/controller/AuthController.kt`
11. Create `infrastructure/web/dto/AuthRequest.kt`, `AuthResponse.kt`
12. Write unit tests with MockK

**Commit message**: `Implement Google OAuth2 authentication with JWT issuance and user upsert`

---

### STORY-002-02: Backend — JWT access token validation filter

**As** the backend system,
**I want** a security filter that validates the JWT on every request,
**So that** only authenticated users can access protected resources.

**Acceptance Criteria:**
- [ ] `BearerTokenAuthenticationFilter` reads `Authorization: Bearer <token>` header
- [ ] Validates JWT signature using RSA public key
- [ ] Validates `iss`, `exp`, `sub` claims
- [ ] Populates `SecurityContextHolder` with a `UserPrincipal(userId, email)`
- [ ] Requests without a valid token to protected routes return `401` with JSON body
- [ ] `SecurityConfig` permits `/api/v1/auth/**` and `/actuator/health` without auth
- [ ] `@AuthenticatedUser` parameter resolver annotation created for controllers
- [ ] Integration test: valid token allows access; missing/expired token returns `401`

**Technical Tasks:**
1. Generate RSA key pair and configure via `@Value("${jwt.private-key}")` and `@Value("${jwt.public-key}")`
2. Configure `SecurityConfig.kt` with `http.oauth2ResourceServer { jwt { } }` or custom filter
3. Create `UserPrincipal.kt` data class
4. Create `AuthenticatedUserArgumentResolver.kt` implementing `HandlerMethodArgumentResolver`
5. Register resolver in `WebMvcConfig.kt`
6. Write integration test with Testcontainers

**Commit message**: `Add JWT validation filter and authenticated user argument resolver`

---

### STORY-002-03: Backend — Refresh token rotation endpoint

**As** the Flutter app,
**I want** to refresh my access token without signing in again,
**So that** the user stays logged in without holding a long-lived access token in memory.

**Acceptance Criteria:**
- [ ] `POST /api/v1/auth/refresh` reads refresh token from `ppp_refresh` cookie
- [ ] Validates token exists in DB and is not expired or revoked
- [ ] Marks old refresh token as revoked
- [ ] Issues new access token + new refresh token (rotation)
- [ ] Sets new refresh token cookie
- [ ] Returns new `{ accessToken }` in body
- [ ] If cookie missing, token revoked, or expired: return `401`
- [ ] `DELETE /api/v1/auth/logout` revokes the refresh token and clears the cookie
- [ ] Unit tests cover: valid rotation, revoked token rejection, expired token rejection

**Technical Tasks:**
1. Add `refreshToken(cookieValue: String): AuthResult` to `AuthUseCase` interface
2. Implement in `AuthUseCaseImpl`
3. Add `revokeRefreshToken(cookieValue: String)` to `AuthUseCase` for logout
4. Add `logout` endpoint to `AuthController`
5. Write unit tests

**Commit message**: `Implement refresh token rotation and logout endpoints`

---

### STORY-002-04: Backend — User profile endpoint

**As** the authenticated user,
**I want** to retrieve and update my profile,
**So that** the app can display my name and avatar and I can correct any data.

**Acceptance Criteria:**
- [ ] `GET /api/v1/users/me` returns `{ id, email, name, pictureUrl, createdAt }`
- [ ] `PATCH /api/v1/users/me` accepts `{ name?: string }` (email and pictureUrl are Google-managed)
- [ ] `userId` is taken from JWT, never from request body
- [ ] Returns `404` if user somehow doesn't exist in DB (defensive)
- [ ] Unit and integration tests

**Technical Tasks:**
1. Create `domain/port/inbound/UserUseCase.kt`: `getProfile(userId): UserProfile`, `updateProfile(userId, name): UserProfile`
2. Create `application/usecase/UserUseCaseImpl.kt`
3. Create `infrastructure/web/controller/UserController.kt`
4. Write tests

**Commit message**: `Add user profile retrieval and update endpoints`

---

### STORY-002-05: Backend — CORS configuration

**As** the Flutter web app,
**I want** the backend to allow cross-origin requests from the frontend domain,
**So that** API calls from the browser are not blocked by the browser's CORS policy.

**Acceptance Criteria:**
- [ ] `CorsConfig.kt` configured exactly as defined in master spec Section 10
- [ ] `allowCredentials = true` (needed for httpOnly cookie)
- [ ] `allowedOrigins` read from `CORS_ALLOWED_ORIGINS` environment variable (comma-separated)
- [ ] Default local origins included when env var not set
- [ ] Preflight `OPTIONS` request to any `/api/**` endpoint returns `200` with correct headers
- [ ] Integration test: sends OPTIONS request and asserts `Access-Control-Allow-Origin` header

**Technical Tasks:**
1. Create `infrastructure/config/CorsConfig.kt`
2. Inject `@Value("\${cors.allowed-origins:http://localhost:3000}")`
3. Write integration test for CORS preflight

**Commit message**: `Configure CORS to allow requests from the Flutter web frontend`

---

### STORY-002-06: Frontend — Google Sign-In screen

**As** an unauthenticated user,
**I want** to see a sign-in screen with a Google button,
**So that** I can authenticate and access my financial data.

**Acceptance Criteria:**
- [ ] `LoginScreen` renders on `/login` route
- [ ] Shows app logo/name "PennyPincher Pro", tagline "Your personal finance command center"
- [ ] "Sign in with Google" button using `flutter_web_auth_2` for PKCE flow
- [ ] While auth is in progress: shows loading indicator, button disabled
- [ ] On success: navigates to `/dashboard`
- [ ] On error: shows error snackbar with message
- [ ] Screen is responsive (centered card on desktop, full-screen on mobile)
- [ ] Design follows color palette from Section 12.2

**Technical Tasks:**
1. Create `features/auth/domain/entity/user_entity.dart` (Freezed)
2. Create `features/auth/domain/repository/auth_repository.dart` interface
3. Create `features/auth/domain/usecase/sign_in_with_google.dart` (returns `Either<Failure, User>`)
4. Create `features/auth/data/datasource/auth_remote_datasource.dart` (calls backend `/auth/google/callback`)
5. Create `features/auth/data/repository/auth_repository_impl.dart`
6. Create `features/auth/presentation/bloc/auth_bloc.dart` with events: `AuthSignInRequested`, `AuthSignOutRequested`, `AuthCheckRequested` and states: `AuthInitial`, `AuthLoading`, `AuthAuthenticated`, `AuthUnauthenticated`, `AuthError`
7. Create `features/auth/presentation/screen/login_screen.dart`
8. Create `features/auth/presentation/widget/google_sign_in_button.dart`
9. Wire up PKCE: generate `code_verifier`, `code_challenge` (S256); open Google auth URL in popup/redirect; catch callback; send code to backend
10. Write BLoC unit tests

**Commit message**: `Add Google Sign-In screen with PKCE OAuth2 flow and auth BLoC`

---

### STORY-002-07: Frontend — Auth guard and token refresh interceptor

**As** the Flutter app,
**I want** all API calls to automatically include the access token and refresh it when expired,
**So that** the user is never unexpectedly logged out mid-session.

**Acceptance Criteria:**
- [ ] `AuthInterceptor` (Dio interceptor) adds `Authorization: Bearer <token>` to every request
- [ ] On `401` response: automatically calls `/auth/refresh` to get a new token, then retries the original request once
- [ ] If refresh also returns `401`: redirects user to `/login`
- [ ] `RouteGuard` (go_router redirect) redirects unauthenticated users to `/login`
- [ ] `RouteGuard` redirects already-authenticated users away from `/login` to `/dashboard`
- [ ] Access token stored in memory (not `localStorage`) using a `TokenService` singleton

**Technical Tasks:**
1. Create `core/network/auth_interceptor.dart` (Dio `QueuedInterceptorsWrapper`)
2. Create `core/auth/token_service.dart` — in-memory token storage with `flutter_secure_storage` backup for page refresh recovery
3. Create `core/router/app_router.dart` with `redirect` callback checking `AuthBloc` state
4. Write unit tests for interceptor retry logic

**Commit message**: `Add auth interceptor with token refresh and route protection guards`

---

### STORY-002-08: Frontend — Splash screen with auth state check

**As** a returning user,
**I want** the app to check if I'm already logged in when it starts,
**So that** I'm not forced to sign in again every time I open the app.

**Acceptance Criteria:**
- [ ] `SplashScreen` shown on app start for 1–2 seconds while checking auth state
- [ ] App dispatches `AuthCheckRequested` event on startup
- [ ] If valid token found in secure storage: restore session (silently refresh if needed) → navigate to `/dashboard`
- [ ] If no token or refresh fails: navigate to `/login`
- [ ] Splash screen shows the app logo and brand colors

**Technical Tasks:**
1. Create `features/auth/presentation/screen/splash_screen.dart`
2. On init: dispatch `AuthCheckRequested` → `TokenService.tryRestoreSession()` → attempt silent refresh
3. Listen to `AuthBloc` state change to navigate
4. Write widget test

**Commit message**: `Add splash screen with automatic session restoration on app startup`

---

## Definition of Done

- [ ] All 8 stories completed and committed
- [ ] Full sign-in flow tested end-to-end in local Docker environment
- [ ] Sign in, navigate to a protected route, wait 15 minutes, observe token auto-refresh
- [ ] Sign out clears tokens and redirects to login
- [ ] Backend security test: raw `curl` without token to any `/api/v1/assets` returns `401`
- [ ] CORS test: preflight from `localhost:3000` origin accepted

## Notes

- Google OAuth2 credentials must be configured in environment before testing end-to-end
- The RSA key pair for JWT signing: generate once, store as env var — do not regenerate on each restart
- `flutter_web_auth_2` uses a redirect URL of `http://localhost:8080/api/v1/auth/google/callback` in dev; must match Google Cloud Console configuration
