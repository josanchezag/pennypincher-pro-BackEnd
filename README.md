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
