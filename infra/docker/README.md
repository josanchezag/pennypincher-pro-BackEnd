# Entorno local con Docker Compose

Levanta PostgreSQL 16, el backend y Adminer con un solo comando. El frontend es opcional.

| Servicio | URL | Notas |
|---|---|---|
| `backend` | http://localhost:8080 | Health: `/actuator/health` · API: `/api/v1` · Swagger UI: `/swagger-ui.html` (perfil `local`) |
| `postgres` | `localhost:5432` | Datos persistentes en el volumen `pgdata` (las llaves locales van en `jwtkeys`) |
| `adminer` | http://localhost:8090 | Solo desarrollo. Servidor `postgres`, usuario y contraseña de `.env.local` |
| `frontend` (opcional) | http://localhost:3000 | Perfil `frontend`; se construye desde `../../../pennypincher-pro-frontend` |

## Requisitos

- Docker 24+ con el plugin Compose v2 (`docker compose version`).
- Para el frontend: los repos `pennypincher-pro-backend` y `pennypincher-pro-frontend` clonados en el mismo directorio padre.

## Inicio rápido

```bash
cd infra/docker
cp .env.local.example .env.local          # ajustar si hace falta; nunca se sube al repo
docker compose --env-file .env.local up -d --build
docker compose --env-file .env.local ps   # postgres y backend deben quedar "healthy"
curl http://localhost:8080/actuator/health
# {"status":"UP",...}
```

El backend espera a que Postgres pase su health check y Flyway aplica las migraciones al arrancar
(`docker compose logs backend | grep "Successfully applied"`).

Con el frontend:

```bash
docker compose --env-file .env.local --profile frontend up -d --build
```

## Operación diaria

```bash
docker compose --env-file .env.local logs -f backend   # ver logs
docker compose --env-file .env.local up -d --build backend   # reconstruir tras cambiar código
docker compose --env-file .env.local down              # apagar conservando los datos
docker compose --env-file .env.local down -v           # apagar y borrar el volumen de Postgres
```

## Notas

- El backend corre con el perfil `local`, que habilita Swagger UI. Con EPIC-002 habilitará además
  `POST /api/v1/auth/dev-login` y generará las llaves RSA al arrancar si `JWT_PRIVATE_KEY_PEM`/`JWT_PUBLIC_KEY_PEM`
  están vacías. Las llaves se guardarán en `/app/keys` (`JWT_LOCAL_KEY_DIR`), montado en el volumen `jwtkeys`
  para que sobrevivan a la recreación del contenedor; `down -v` también lo borra.
- El frontend recibe en su build `API_ORIGIN` (`FRONTEND_API_ORIGIN`, por defecto `http://localhost:8080`),
  `API_BASE_URL` (`<origen>/api/v1`), `GOOGLE_CLIENT_ID` y `DEV_LOGIN`.
- Para probar como en producción, usar `SPRING_PROFILES_ACTIVE=prod` y definir todas las variables.
- Si un puerto está ocupado, cambiar `BACKEND_PORT`, `POSTGRES_PORT`, `ADMINER_PORT` o `FRONTEND_PORT` en `.env.local`.
- Adminer es solo para desarrollo y no forma parte de los manifiestos de Kubernetes.
