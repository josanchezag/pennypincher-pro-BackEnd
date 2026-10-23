# PennyPincher Pro · Backend

API REST en **Kotlin + Spring Boot** para la gestión de finanzas personales del hogar: fortuna neta,
presupuesto mensual, detalle de deudas y gastos, ahorros y metas de independencia financiera.

Este repositorio también contiene la **documentación del proyecto** (`docs/`) y la **infraestructura**
(`infra/`: Docker Compose para desarrollo local y manifiestos de Kubernetes). No existe un repositorio
de infraestructura aparte.

> Frontend (Flutter Web): [pennypincher-pro-frontend](https://github.com/josanchezag/pennypincher-pro-frontend)

## Stack

| Pieza | Tecnología |
|---|---|
| Lenguaje y framework | Kotlin 1.9.25 · Spring Boot 3.3 · JDK 21 · Gradle (Kotlin DSL, wrapper incluido) |
| Persistencia | PostgreSQL 16 · Spring Data JPA · migraciones con **Flyway** (`src/main/resources/db/migration`) |
| Seguridad | Spring Security 6 · CORS configurable · login con **Google OAuth2** (PKCE) y tokens propios **RS256** |
| Resiliencia | Resilience4j: circuit breaker sobre el endpoint de tokens de Google |
| Documentación de la API | springdoc-openapi (Swagger UI solo en el perfil `local`) |
| Pruebas | JUnit 5 · MockK · Testcontainers (o una base Postgres externa vía `TEST_DB_URL`) · JaCoCo |
| Calidad | ktlint · cobertura mínima de 80 % en los paquetes `domain` y `application` |
| Contenedores | Dockerfile multi-stage · Docker Compose · manifiestos de Kubernetes (k3s) |

## Arquitectura

Monolito modular con arquitectura **hexagonal** (puertos y adaptadores). Cada módulo funcional
(`auth`, `user`, `networth`, `budget`, `expenses`, `goals`, `dashboard`) y el módulo transversal
`shared` siguen la misma estructura de capas:

```
com.pennypincher.pro.<módulo>
├── domain/          # Modelo y puertos en Kotlin puro: sin Spring, JPA ni Jakarta
├── application/     # Casos de uso: implementan los puertos de entrada
└── infrastructure/  # Adaptadores: web (controllers), persistencia (JPA), seguridad, configuración
```

El aislamiento entre usuarios es por fila: toda tabla con datos del usuario tiene `user_id`, y un
recurso de otro usuario responde `404`. Las decisiones de arquitectura están en `docs/adr/`.

## Estructura del repositorio

```
.
├── docs/
│   ├── DECISIONES-Y-PLAN-DE-EJECUCION.md   # Decisiones vigentes (prevalecen sobre el spec)
│   ├── PENNYPINCHER_PRO_MASTER_PROMPT.md   # Especificación maestra
│   ├── adr/                                # Architecture Decision Records
│   └── epics/                              # Épicas e historias (EPIC-001 … EPIC-008)
├── infra/
│   ├── docker/                             # docker-compose.yml + .env.local.example
│   └── kubernetes/                         # namespace, backend, frontend y database
├── src/                                    # Código del backend y migraciones Flyway
└── .github/workflows/                      # CI del backend y validación de manifiestos
```

## Inicio rápido con Docker Compose

Requisitos: Docker con el plugin Compose. Levanta Postgres 16, el backend y Adminer:

```bash
cd infra/docker
cp .env.local.example .env.local
docker compose --env-file .env.local up -d --build

curl http://localhost:8080/actuator/health   # {"status":"UP"}
# Adminer (solo desarrollo): http://localhost:8090
```

Para apagar todo y borrar los datos: `docker compose --env-file .env.local down -v`.
Más detalles en [`infra/docker/README.md`](infra/docker/README.md).

## Ejecución local con Gradle

Requisitos: JDK 21 y una base Postgres 16 accesible (por ejemplo, la del Compose):

```bash
./gradlew bootRun          # usa el perfil `local` por defecto
```

Sin variables de entorno, el perfil `local` se conecta a `jdbc:postgresql://localhost:5432/pennypincher`
con usuario y contraseña `pennypincher`.

## Perfiles

| Perfil | Uso |
|---|---|
| `local` (por defecto) | Desarrollo. Valores por defecto para localhost, Swagger UI, `dev-login` y llaves RSA generadas al arrancar en `jwt.local-key-dir` si no se configuran. |
| `test` | Pruebas automatizadas. La base la aporta Testcontainers o `TEST_DB_URL`. |
| `prod` | Producción. Todos los secretos son obligatorios y llegan por variables de entorno. Swagger deshabilitado. |

## Variables de entorno

Nunca se suben valores reales al repositorio. Plantilla para desarrollo: `infra/docker/.env.local.example`.

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local`, `test` o `prod` |
| `DATABASE_URL` | JDBC, p. ej. `jdbc:postgresql://<host>/<db>?sslmode=require` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Credenciales de Postgres |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Credenciales OAuth2 de Google (cliente de tipo aplicación web). En `prod` son obligatorias: si faltan, la aplicación no arranca |
| `GOOGLE_ALLOWED_REDIRECT_URIS` | Redirect URIs permitidos, separados por comas (local: `http://localhost:3000/auth.html`; obligatorio en `prod`) |
| `JWT_PRIVATE_KEY_PEM` / `JWT_PUBLIC_KEY_PEM` | Par RSA en PEM o PEM codificado en Base64 (obligatorio en `prod`) |
| `JWT_LOCAL_KEY_DIR` | Solo `local`: carpeta donde se generan y reutilizan las llaves RSA (por defecto `build/keys`) |
| `AUTH_COOKIE_SECURE` / `AUTH_COOKIE_SAME_SITE` | Atributos de la cookie `ppp_refresh` (por defecto `true` y `Strict`) |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend separados por comas, p. ej. `https://pennypincher-pro.vercel.app` |
| `PORT` / `SERVER_PORT` | Puerto HTTP (Render inyecta `PORT`; por defecto `8080`) |

## Autenticación

Base de la API: `/api/v1`. Las respuestas usan el sobre `{ data, meta: { timestamp, requestId } }` y los
errores `{ error: { code, message, details? }, meta }`. `requestId` repite el header `X-Request-ID` si es un
UUID válido; si no, se genera uno.

### Flujo con Google (authorization code + PKCE)

1. El frontend abre Google con `response_type=code`, `code_challenge` (S256) y `redirect_uri`
   `http://localhost:3000/auth.html`.
2. Envía el código a `POST /api/v1/auth/google/callback` con `{ code, redirectUri, codeVerifier }`.
3. El backend comprueba que `redirectUri` esté en `GOOGLE_ALLOWED_REDIRECT_URIS`, intercambia el código en
   `https://oauth2.googleapis.com/token` (con `client_secret` y `code_verifier`) y valida el ID token con el
   JWKS de Google: firma, `iss` (`accounts.google.com`), `aud` (el client id), `exp` y `email_verified`.
4. Crea el usuario o actualiza su e-mail, nombre y foto (búsqueda por `google_sub`) y responde
   `{ accessToken, tokenType: "Bearer", expiresIn: 900, user }` con la cookie `ppp_refresh`.

Errores: `400 VALIDATION_FAILED` (campos vacíos o `redirectUri` no permitido), `401 GOOGLE_AUTH_FAILED`
(código o ID token inválido) y `503 IDENTITY_PROVIDER_UNAVAILABLE` (Google caído o circuit breaker
abierto: Resilience4j, instancia `google-token`).

### Tokens

- **Access token**: JWT **RS256** de 15 minutos con `sub` (id del usuario), `email`, `iss`
  (`pennypincher-pro`), `iat` y `exp`. El frontend lo guarda solo en memoria.
- **Refresh token**: valor opaco aleatorio de 256 bits; en la base solo se guarda su SHA-256
  (tabla `refresh_tokens`). Vigencia de 30 días. Viaja en la cookie `ppp_refresh`: `HttpOnly`, `Secure`
  (`AUTH_COOKIE_SECURE`), `SameSite` (`AUTH_COOKIE_SAME_SITE`: `Strict` por defecto; `None` exige `Secure`),
  `Path=/api/v1/auth`.
- **Llaves RSA**: `JWT_PRIVATE_KEY_PEM` (PKCS#8) y `JWT_PUBLIC_KEY_PEM` (X.509), en PEM o PEM en Base64.
  Obligatorias en `prod`. En `local`, si faltan, se generan una sola vez en `JWT_LOCAL_KEY_DIR`
  (por defecto `build/keys`). Para generarlas:

  ```bash
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
  openssl rsa -in private.pem -pubout -out public.pem
  export JWT_PRIVATE_KEY_PEM="$(base64 -w0 private.pem)" JWT_PUBLIC_KEY_PEM="$(base64 -w0 public.pem)"
  ```

### Refresh y logout

- `POST /api/v1/auth/refresh` lee la cookie `ppp_refresh`, revoca ese refresh token y emite un par nuevo
  (misma respuesta que el callback y cookie nueva). Si la cookie falta, es desconocida, venció o fue revocada,
  responde `401 UNAUTHORIZED` y borra la cookie.
- Refrescos concurrentes (por ejemplo, dos pestañas a la vez): solo uno gana. Si se presenta un token rotado
  hace menos de `auth.refresh.reuse-grace` (30 s), la respuesta es `401` con el código
  `REFRESH_SUPERSEDED`, **sin** borrar la cookie ni cerrar la sesión ganadora: el navegador ya tiene la
  cookie nueva, así que el cliente debe reintentar el refresh una sola vez.
- Reutilización: presentar un token rotado después de ese margen se trata como robo y se eliminan todos los
  refresh tokens del usuario (se cierran todas sus sesiones).
- `DELETE /api/v1/auth/logout` elimina el refresh token de la cookie (idempotente, también sin cookie) y
  responde `204` borrando la cookie. Un token cerrado con logout que se vuelva a presentar es simplemente
  desconocido (`401`) y no afecta a las sesiones de otros dispositivos.
- Protección CSRF: como estos dos endpoints se autentican solo con la cookie, exigen un header `Origin`
  incluido en `CORS_ALLOWED_ORIGINS`; si falta o no está permitido, responden `403 FORBIDDEN_ORIGIN`.

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/refresh -b cookies.txt -c cookies.txt -H 'Origin: http://localhost:3000'
curl -i -X DELETE http://localhost:8080/api/v1/auth/logout -b cookies.txt -H 'Origin: http://localhost:3000'
```

### Rutas protegidas

Todo endpoint exige `Authorization: Bearer <accessToken>`, salvo `/api/v1/auth/**` y `/actuator/health/**`
(y Swagger en `local`). Sin token, o con un token vencido, mal firmado o de otro emisor, la respuesta es
`401` con `{ "error": { "code": "UNAUTHORIZED", ... } }`. En las rutas públicas se ignora el header
`Authorization`, así un token vencido no bloquea `/auth/refresh`. Los controladores reciben al usuario con
`@AuthenticatedUser principal: UserPrincipal` (`userId`, `email`).

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/dev-login | jq -r .data.accessToken)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me
```

### Perfil del usuario

- `GET /api/v1/users/me` → `{ id, email, name, pictureUrl, createdAt }` del usuario del token.
- `PATCH /api/v1/users/me` con `{ "name": "..." }`: el nombre se recorta y debe tener entre 1 y 100
  caracteres (si no, `400 VALIDATION_FAILED`). `email` y `pictureUrl` los gestiona Google y se ignoran.
- El `userId` siempre sale del access token, nunca del cuerpo. Si el usuario ya no existe: `404 NOT_FOUND`.

### Patrimonio: activos

Los montos son COP con dos decimales: en JSON van como número (`350000000.00`), se aceptan valores `>= 0`
con hasta 16 dígitos enteros y 2 decimales, y el nombre se recorta y debe tener entre 1 y 150 caracteres.

- `GET /api/v1/assets[?category=REAL_ESTATE]` → lista de `{ id, name, value, category, createdAt, updatedAt }`
  activos del usuario, ordenada por categoría (en el orden del enum) y luego por nombre sin distinguir
  mayúsculas. Categorías: `REAL_ESTATE`, `VEHICLE`, `BUSINESS`, `CASH`, `SAVINGS`, `EQUIPMENT`, `OTHER`.
- `GET /api/v1/assets/total` → `{ totalAssets }`: suma de los activos activos.
- `GET /api/v1/assets/{id}` → el activo (es la URL del header `Location` de la creación).
- `POST /api/v1/assets` con `{ name, value, category }` → `201` con `Location` y el activo creado.
- `PUT /api/v1/assets/{id}` con el mismo cuerpo → `200` con el activo actualizado.
- `DELETE /api/v1/assets/{id}` → `204`; es un borrado lógico (`is_active = false`). Solo esa operación cambia
  `is_active`: una actualización que llega después del borrado no reactiva el activo.
- Un activo de otro usuario, inexistente o ya borrado responde `404 NOT_FOUND`. Un cuerpo inválido, una
  categoría desconocida (también en `?category=`) o un id que no es UUID responden `400 VALIDATION_FAILED`
  con el campo en `error.details`.

### Patrimonio: pasivos

`/api/v1/liabilities` funciona igual que `/api/v1/assets` (misma implementación: `CategorizedCrudController`
con `/total` opcional vía `TotalEndpoint`, y `AbstractTotalCrudUseCase`), pero el monto se llama
`currentBalance` (saldo pendiente, `>= 0`):

- `GET /api/v1/liabilities[?category=CREDIT_CARD]` → `{ id, name, currentBalance, category, createdAt, updatedAt }`.
  Categorías: `CREDIT_CARD`, `PERSONAL_LOAN`, `BANK_LOAN`, `FAMILY_DEBT`, `OTHER`.
- `GET /api/v1/liabilities/total` → `{ totalLiabilities }`; `GET /api/v1/liabilities/{id}` → el pasivo.
- `POST /api/v1/liabilities` y `PUT /api/v1/liabilities/{id}` con `{ name, currentBalance, category }`;
  `DELETE /api/v1/liabilities/{id}` → `204` (borrado lógico).

### Patrimonio neto

- `GET /api/v1/net-worth/current` → `{ totalAssets, totalLiabilities, netWorth }` calculado en vivo con los
  activos y pasivos activos (no guarda nada). `netWorth` puede ser negativo.
- `POST /api/v1/net-worth/snapshots` con `{ "snapshotDate": "2026-10-21" }` guarda los totales actuales
  para el **mes** de esa fecha: `snapshotDate` se normaliza al primer día del mes. Si el mes no tenía
  snapshot responde `201`; si ya existía lo recalcula (mismo `id`, conserva `createdAt`) y responde `200`.
  Es un único `INSERT ... ON CONFLICT DO UPDATE`, así que varias peticiones simultáneas para el mismo mes
  dejan una sola fila y todas responden `201`/`200`. Un mes posterior al actual (UTC) responde
  `400 VALIDATION_FAILED` en `snapshotDate`, y unos totales que no caben en `NUMERIC(18,2)` responden
  `400` en `totalAssets`/`totalLiabilities`.
- `GET /api/v1/net-worth/snapshots[?from=2024-01-01&to=2026-12-31]` → lista de
  `{ id, snapshotDate, totalAssets, totalLiabilities, netWorth, createdAt, updatedAt }` ordenada por
  `snapshotDate` ascendente. Los límites son inclusivos y opcionales; `from` se lleva al primer día de su
  mes. `from` posterior a `to`, o una fecha que no es ISO (`yyyy-MM-dd`), responde `400 VALIDATION_FAILED`.

### Detalle de gastos y ahorro

Cinco recursos con el mismo CRUD que los activos (`GET` lista, `GET /{id}`, `POST` → `201` con `Location`,
`PUT`, `DELETE` → `204` con borrado lógico, `404` para registros de otro usuario o borrados) y un
`GET .../summary` calculado en la base de datos con las filas activas. No tienen `/total`. Montos COP con dos
decimales (`>= 0`, hasta 16 dígitos enteros); nombre recortado de 1 a 150 caracteres.

| Recurso | Cuerpo | Orden | `summary` |
|---|---|---|---|
| `/api/v1/credit-cards` | `{ name, monthlyPayment, totalBalance }` | nombre | `{ totalMonthlyPayments, totalOutstandingBalance, count }` |
| `/api/v1/loans` | `{ name, monthlyPayment, totalBalance, originalAmount? }` | nombre | igual que tarjetas |
| `/api/v1/fixed-expenses[?category=]` | `{ name, monthlyAmount, category }` | categoría, nombre | `{ totalMonthlyAmount, count, byCategory: [{ category, totalMonthlyAmount, count }] }` |
| `/api/v1/variable-expenses[?category=]` | `{ name, monthlyAmount, category }` | categoría, nombre | igual que gastos fijos |
| `/api/v1/savings-accounts` | `{ name, monthlyContribution, currentBalance }` | nombre | `{ totalMonthlyContributions, totalCurrentBalance, count }` |

- El nombre se ordena sin distinguir mayúsculas; la categoría sigue el orden del enum.
- Gastos fijos: `FOOD, EDUCATION, TRANSPORT, UTILITIES, INSURANCE, HOUSING, HEALTH, CHARITY, COMMUNICATION,
  TAXES, OTHER`. Gastos variables: `LEISURE, STREAMING, HOUSEHOLD_STAFF, DINING, CLOTHING, GIFTS, OTHER`.
  Una categoría desconocida (en el cuerpo o en `?category=`) responde `400 VALIDATION_FAILED`.
- `byCategory` solo lista categorías con gastos activos, en el orden del enum, e ignora `?category=`.
- `originalAmount` del préstamo es opcional: la respuesta siempre lo incluye (`null` si no se registra) y un
  `PUT` sin él lo borra. No se exige que sea mayor que el saldo.
- El presupuesto mensual suma estas tablas (pagos de tarjetas y préstamos, gastos fijos separando `CHARITY`,
  gastos variables y aportes de ahorro), pero solo cambia al crearlo, editarlo o con `POST .../recalculate`.
- Implementación: tarjetas, préstamos y ahorro comparten `AbstractBalanceAccountUseCase`,
  `BalanceAccountEntity` y `UncategorizedCrudController`; gastos fijos y variables usan el mismo
  `Expense<C>`, `ExpenseUseCaseImpl` y `ExpenseController`. `/summary` se habilita con `SummaryEndpoint`.

### Metas financieras

`/api/v1/financial-goals` tiene el mismo CRUD que los demás recursos (`GET` lista, `GET /{id}`, `POST` → `201`
con `Location`, `PUT`, `404` para metas de otro usuario), pero `DELETE` → `204` es un **borrado físico**: la
fila desaparece (las metas son hitos intencionales, no historial). Un `PUT` que llega después del borrado
responde `404` y no vuelve a crear la fila: la actualización es un único `UPDATE` filtrado por id y usuario.

- Cuerpo: `{ name, level, targetAmount, monthlyInvestment, expectedAnnualReturn, targetYears? }`.
  `targetAmount > 0` y `monthlyInvestment >= 0` (COP, dos decimales); `expectedAnnualReturn` es una fracción
  `0 < v <= 1` con hasta 4 decimales (`0.11` = 11 %); `targetYears` es opcional, entero de 1 a 100, y un `PUT`
  sin él lo borra. La respuesta agrega `levelDescription` y siempre incluye `targetYears` (`null` si no hay).
- Niveles, en este orden fijo: `FINANCIAL_SECURITY`, `FINANCIAL_INDEPENDENCE`, `FINANCIAL_FREEDOM`,
  `ABSOLUTE_FREEDOM`. Son una clasificación: varias metas pueden compartir nivel. `GET .../levels` devuelve
  `[{ level, description }]`; las descripciones son constantes del backend, no están en la base.
- La lista se ordena por nivel, luego por `targetAmount` ascendente y luego por nombre sin distinguir mayúsculas.
- `POST .../defaults` crea las cuatro metas del Excel (Seguridad Financiera 605.000.000 con 1.470.000/mes,
  Independencia Financiera 1.016.000.000 con 3.040.000, Libertad Financiera 2.066.000.000 con 7.000.000 y
  Libertad Absoluta 4.366.000.000 con 15.850.000; todas al 11 % anual y 16 años), solo para los niveles que
  aún no tienen meta. Es idempotente y responde `200` con la lista completa.
- `GET .../{id}/projection` (`404` si es de otro usuario) y `GET .../all-projections` (todas, en el orden de la
  lista; `[]` sin metas) devuelven `{ goalId, name, level, targetAmount, currentNetWorth, progressAmount,
  progressPercentage, remainingAmount, monthlyInvestment, expectedAnnualReturn, targetYears, reachable,
  projectedMonthsToGoal, projectedYearsToGoal, projectedAchievementDate, projectionDataPoints: [{ month, date,
  projectedValue }] }`. El patrimonio neto se lee en vivo (activos − pasivos activos; puede ser negativo).
- Cálculo (`GoalProjectionService`, Kotlin puro con `BigDecimal` en `DECIMAL128`; solo se redondea al final):
  tasa mensual efectiva `r = (1 + anual)^(1/12) − 1` y `FV(n) = PV·(1+r)^n + PMT·((1+r)^n − 1)/r`, con el
  aporte al final de cada mes.
  La meta se alcanza en el primer mes `n` (0 a 600) con `FV(n) >= meta`; si no llega en 600 meses,
  `reachable` es `false` y los tres campos `projected*` son `null`. El progreso es el patrimonio limitado a
  `[0, meta]`, el porcentaje va de 0 a 100 con dos decimales (`HALF_UP`) y los años son `meses / 12`. Las fechas
  son el día 1 del mes actual (UTC) más `n` meses. Los puntos van del mes 0 (el patrimonio actual) hasta el mes
  en que se alcanza la meta, con un mínimo de 12 meses, o hasta el 600 si no se alcanza.
- Implementación: `FinancialGoalUseCaseImpl` extiende `AbstractCrudUseCase` y el controlador
  `UncategorizedCrudController`. El borrado lo decide el puerto (`UserScopedCrudRepository.delete`): lógico en
  los adaptadores de `OwnedRecordRepositoryAdapter`, físico en `FinancialGoalRepositoryAdapter`. El puerto
  separa `insert` y `update`; `update` devuelve `null` si la fila ya no existe y el caso de uso responde `404`.
- En todos los recursos, un número enviado como texto (`"5"`) o fuera del rango del tipo
  (`"targetYears": 99999999999`) responde `400 VALIDATION_FAILED` con el campo en `error.details`.

### CORS

Registrado solo para `/api/**`, igual que en la sección 10 de la especificación: orígenes explícitos de
`CORS_ALLOWED_ORIGINS` (separados por comas; sin la variable se usan `http://localhost:3000` y
`http://localhost:8080`; nunca `*`), métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`, headers
`Authorization, Content-Type, X-Request-ID`, `allowCredentials = true` (necesario para la cookie) y
`maxAge = 3600`. Se exponen `Location` y `X-Request-ID`. Un origen inválido detiene el arranque.

```bash
curl -i -X OPTIONS http://localhost:8080/api/v1/users/me \
  -H 'Origin: http://localhost:3000' -H 'Access-Control-Request-Method: GET'
```

En producción el frontend (Vercel) y la API (Render) están en sitios distintos: la cookie necesitará
`AUTH_COOKIE_SAME_SITE=None` (con `Secure`), y el header `Origin` obligatorio en refresh/logout la protege
contra CSRF.

### Configurar el cliente OAuth de Google

1. En [Google Cloud Console](https://console.cloud.google.com/apis/credentials) crear un
   **ID de cliente de OAuth** de tipo **Aplicación web** (y configurar la pantalla de consentimiento con los
   scopes `openid`, `email` y `profile`).
2. **Orígenes autorizados de JavaScript**: `http://localhost:3000` (y el dominio del frontend en producción).
3. **URI de redireccionamiento autorizados**: `http://localhost:3000/auth.html` (y
   `https://<frontend>/auth.html` en producción). Deben coincidir con `GOOGLE_ALLOWED_REDIRECT_URIS`.
4. Copiar el client id y el secret en `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` (`infra/docker/.env.local`).
   El frontend usa el mismo client id.

### Dev login (solo perfil `local`)

`POST /api/v1/auth/dev-login` emite el mismo par de tokens para un usuario fijo de prueba
(`dev@pennypincher.local`, "Dev User") sin pasar por Google. En `prod` el endpoint no existe (`404`).

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/dev-login -c cookies.txt
```

## Pruebas y calidad

```bash
./gradlew ktlintCheck test build
```

Las pruebas de integración usan Testcontainers (requiere Docker). Si Docker no está disponible,
se puede apuntar a una base Postgres 16 existente:

```bash
TEST_DB_URL=jdbc:postgresql://localhost:5432/pennypincher_test \
TEST_DB_USERNAME=postgres TEST_DB_PASSWORD=postgres ./gradlew test
```

El reporte de cobertura queda en `build/reports/jacoco/test/html/index.html`.

## Despliegue

Objetivo de capa gratuita: API en **Render** (Docker), base en **Neon** (Postgres serverless) y
frontend en **Vercel**. Los manifiestos de `infra/kubernetes/` permiten un despliegue futuro en un
clúster k3s. Por ahora no hay nada publicado; la guía está en la sección 16 de la especificación.

> En el plan gratuito de Render el servicio se suspende por inactividad; la primera petición tras un
> periodo sin uso tarda unos segundos.
