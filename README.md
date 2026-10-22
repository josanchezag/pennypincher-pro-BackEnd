# PennyPincher Pro · Backend

API REST en **Kotlin** para la gestión de finanzas personales del hogar (fortuna neta, presupuesto mensual, deudas, ahorros y metas financieras).

> Frontend: [pennypincher-pro-frontend](https://github.com/josanchezag/pennypincher-pro-frontend)

## Stack

- Kotlin + Spring Boot (Gradle Kotlin DSL)
- PostgreSQL + migraciones con Flyway
- Seguridad: **OAuth2 Resource Server** validando ID tokens de **Google** (JWT)
- CORS restringido a los orígenes del frontend
- Docker para despliegue

## Requisitos

- JDK 21
- Docker (para Postgres local)

## Configuración

Variables de entorno (ver `.env.example`; **nunca** subir `.env` al repo):

| Variable | Descripción |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/pennypincher` |
| `DB_USER` / `DB_PASSWORD` | Credenciales de Postgres |
| `GOOGLE_CLIENT_ID` | Client ID de Google (audiencia esperada del token) |
| `CORS_ALLOWED_ORIGINS` | Lista separada por comas, p. ej. `http://localhost:5000,https://pennypincher.pages.dev` |
| `PORT` | Puerto HTTP (Render lo inyecta) |

## Ejecución local

```bash
docker run -d --name pp-db -p 5432:5432 \
  -e POSTGRES_DB=pennypincher -e POSTGRES_USER=pp -e POSTGRES_PASSWORD=pp \
  postgres:16

./gradlew bootRun
```

## Tests

```bash
./gradlew test
```

## Despliegue (gratuito)

| Pieza | Servicio |
|---|---|
| API | **Render** (Web Service con Docker) |
| Base de datos | **Neon** o **Supabase** (Postgres gratuito) |

> En el plan gratuito de Render el servicio se suspende por inactividad; la primera petición tras un periodo sin uso tarda unos segundos.

## Seguridad

- Todos los endpoints (excepto `/actuator/health`) requieren `Authorization: Bearer <google-id-token>`.
- Los datos se aíslan por hogar: cada usuario solo accede a la información de su hogar.
- CORS solo acepta los orígenes configurados en `CORS_ALLOWED_ORIGINS`.

## Estado

🚧 En construcción.
