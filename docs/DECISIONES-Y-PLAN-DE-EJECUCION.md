# PennyPincher Pro — Decisiones y plan de ejecución

> Este documento **prevalece** sobre `PENNYPINCHER_PRO_MASTER_PROMPT.md` y las épicas cuando hay conflicto.
> Lo deben leer primero la sesión orquestadora y todos los agentes.

## 0. Arranque de una sesión nueva (checklist del orquestador)

1. Verificar la red (debe devolver un código distinto de `000`):
   ```bash
   for u in https://repo.maven.apache.org/maven2/ https://plugins.gradle.org/m2/ https://services.gradle.org/distributions/ \
            https://storage.googleapis.com/flutter_infra_release/releases/releases_linux.json https://pub.dev/api/packages/flutter_bloc; do
     printf "%-80s %s\n" $u "$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 $u)"; done
   ```
   Si sale `000` o el proxy responde 403, el entorno no tiene **Network access = Trusted/Completo**. Detenerse y avisarle al usuario.
2. Repos: `josanchezag/pennypincher-pro-BackEnd` y `josanchezag/pennypincher-pro-frontEnd`, con rama por defecto `master`. Clonar ambos al mismo directorio padre.
3. Herramientas del entorno: JDK 21 y Gradle 8.14 ya vienen instalados, junto con el cliente `psql` 16 y los binarios del servidor en `/usr/lib/postgresql/16/bin`, y Docker CLI (hay que iniciar el daemon con `sudo dockerd &` si se necesita). Flutter **no** viene instalado: hay que instalar Flutter 3.24.x desde `storage.googleapis.com`.
4. La máquina tiene 2 CPU y 7 GB de RAM. No conviene correr al mismo tiempo dos builds pesados de Gradle o Flutter. Usar `org.gradle.jvmargs=-Xmx1536m`.

## 1. Decisiones tomadas (no requieren aprobación del usuario)

1. **Sin repo de infra** (decisión del usuario):
   - En `pennypincher-pro-BackEnd`: `docs/` (spec, épicas y ADR), `infra/docker/docker-compose.yml` (Postgres, backend y Adminer; el frontend es opcional), `infra/docker/.env.local.example`, `infra/kubernetes/**`, y el workflow `validate-k8s.yml` dentro de `.github/workflows/`.
   - En `pennypincher-pro-frontEnd`: `Dockerfile`, `nginx.conf` y su propio CI.
   - Las rutas del spec que dicen `pennypincher-pro-infra/...` se mapean a esas ubicaciones.
2. **Flyway** dentro del backend, en `src/main/resources/db/migration`. Nunca se modifica una migración ya aplicada.
3. **Ramas**: `master` en lugar de `main`. Cada historia va en su rama `story/E00X-S0YY-slug`, se commitea, la revisa el reviewer, se corrige y se fusiona a `master` con `--no-ff` o fast-forward. Los workflows de CI se disparan con `push`/`pull_request` sobre `master`.
4. **Commits**: siguen la sección 14 del spec (inglés, frases normales, sin Conventional Commits, sin mencionar IA ni herramientas). Autor git: `Claude <noreply@anthropic.com>`, porque la plataforma lo exige. No se agrega la línea `Co-Authored-By`: el usuario pide explícitamente que los mensajes no mencionen herramientas.
5. **Versiones** del spec: Kotlin 1.9.25, Spring Boot 3.3.x, JDK 21, Gradle wrapper 8.x, Flutter 3.24.x, y las librerías indicadas en la sección 2.1.
6. **Arquitectura**: monolito modular hexagonal, como define el spec.
   - **Se aplican**:
     - **Template Method**: `AbstractCrudUseCase<T, Cmd>` para las 8 entidades CRUD casi idénticas (Asset, Liability, IncomeSource, CreditCard, Loan, FixedExpense, VariableExpense, SavingsAccount), con hooks de validación y de construcción. En el frontend, un `CrudBloc<T>` genérico y widgets de lista/formulario parametrizados. Es la forma principal de evitar código duplicado.
     - **Strategy**: cálculo de porcentajes del presupuesto (`BudgetComponentStrategy` por componente: tarjetas, préstamos, gastos, ahorro, caridad) y validadores por tipo.
     - **Adapter**: adapters JPA hacia los puertos del dominio, y `GoogleTokenClient` hacia el puerto `IdentityProviderPort`.
     - **Facade**: `DashboardUseCase` como agregador. En el frontend, una facade de API por feature si aplica.
     - **Factory**: mappers entre capas, y `RefreshCookieFactory`.
     - **Observer**: BLoC y streams en el frontend. En el backend, si hace falta, eventos de dominio en memoria (no se requieren en la fase 1).
     - **Singleton**: beans de Spring y GetIt (`lazySingleton`).
     - **Builder** y **Decorator**: como indica el spec en la sección 5.4.
     - **Circuit Breaker**: Resilience4j (`resilience4j-spring-boot3`) en la llamada al endpoint de tokens de Google. Si el circuito está abierto, responde 503 con un sobre de error.
   - **No se aplican**: API Gateway, Database per Service, Saga y Service Discovery. Son patrones de microservicios y no se justifican en una sola instancia gratuita de Render con una base Neon de 0.5 GB. Aumentarían los arranques en frío y la complejidad (transacciones distribuidas) sin ningún beneficio de negocio. Lo que resolvería un gateway (autenticación, CORS, enrutamiento) lo cubre la cadena de filtros de Spring Security. Los módulos quedan aislados por paquete, de modo que se puedan separar más adelante.
7. **Perfil `local` con "dev login"**: `POST /api/v1/auth/dev-login`, disponible **solo** con el perfil `local` (`@Profile("local")`). Emite el mismo par de tokens para un usuario de prueba, así se puede probar sin Google Cloud Console. Una prueba debe verificar que el endpoint no existe en `prod`. En el frontend, el botón "Dev login" se muestra solo cuando `--dart-define=DEV_LOGIN=true`.
8. **Pruebas de integración**: Testcontainers si Docker funciona y tiene acceso a Docker Hub. Si no, Postgres 16 local con `initdb`/`pg_ctl` desde `/usr/lib/postgresql/16/bin`, y las pruebas leen `TEST_DB_URL`. Se implementa con una abstracción que permita usar cualquiera de las dos opciones.
9. **Sin despliegue por ahora**: de la EPIC-008 se hace solo lo local. Se escriben `render.yaml` y `vercel.json` como archivos, pero nada se publica en Render, Vercel ni Neon.
10. **RSA**: en el perfil `local` se generan las llaves al arrancar si no existen las variables, y se guardan en `build/` (que no se versiona). En `prod` las llaves son obligatorias y vienen por variables de entorno.

## 2. Proceso con agentes, por épica

Para cada épica, en orden estricto del 001 al 008:

1. El orquestador lanza **en paralelo**:
   - **Agente BACK**: trabaja solo en el repo backend. Implementa todas las historias de backend de la épica, una rama y un commit por historia. Antes de entregar cada historia ejecuta `./gradlew ktlintCheck test build`.
   - **Agente FRONT**: trabaja solo en el repo frontend. Implementa todas las historias de frontend de la épica, una rama y un commit por historia. Antes de entregar ejecuta `dart format --set-exit-if-changed . && flutter analyze --fatal-infos && flutter test && flutter build web --release`.
   - Se coordinan por el contrato de la API (secciones 8 y 9 del spec y los JSON de las épicas). Si algo es ambiguo, el contrato del backend manda.
2. **Agente REVIEWER**: por cada historia terminada hace un code review **exhaustivo**:
   - Revisa cada AC y DoD de la historia, uno por uno, con evidencia (archivo:línea, o la salida del comando que lo prueba).
   - Revisa los NFR del spec: arquitectura hexagonal y las reglas duras de las secciones 5.2 y 5.3, SOLID, Clean Code, límites de la sección 5.6 (funciones de máximo 30 líneas, clases de máximo 200, sin magic numbers, sin TODOs, sin código comentado), aislamiento multi-tenant (devolver 404 cuando el recurso es de otro usuario), seguridad (tokens, cookies, CORS) y cobertura de al menos 80% en domain y application.
   - Verifica que no haya **código duplicado** y que los patrones de la sección 1.6 estén aplicados donde corresponde, sin sobreingeniería.
   - Corre por su cuenta los builds y los tests. No confía en lo que reportan los otros agentes.
   - Entrega hallazgos con severidad (BLOCKER/MAJOR/MINOR) y la corrección esperada.
3. Los hallazgos BLOCKER y MAJOR se devuelven al agente correspondiente para que los corrija, y el reviewer verifica de nuevo. Cuando la historia queda aprobada, se fusiona a `master` y se hace `push`.
4. El **orquestador** supervisa: si un agente se cuelga o falla, lo relanza con el contexto de lo que alcanzó a hacer. No se empieza la épica N+1 sin cerrar la N.
5. Al final de cada épica, el orquestador le informa al usuario las decisiones menores que tomó.
