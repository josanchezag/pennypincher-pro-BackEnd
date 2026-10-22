# ADR-001 — Technology stack and architectural style

**Status**: Accepted
**Date**: 2026-10-21

## Context

PennyPincher Pro replaces a personal Excel budget with a responsive web application. It is used by a
single person today, must stay within free hosting tiers, and must keep data isolated per user so it
can grow into a multi-tenant product later.

## Decision

- **Frontend**: Flutter Web 3.24 with BLoC, go_router, dio, get_it/injectable and fl_chart.
- **Backend**: Kotlin 1.9 on Spring Boot 3.3, hexagonal architecture (ports and adapters), JPA/Hibernate.
- **Database**: PostgreSQL 16, schema managed by Flyway inside the backend repository.
- **Authentication**: Google OAuth2 (authorization code + PKCE). The backend issues its own RS256
  access tokens (15 min) and rotating opaque refresh tokens stored hashed in the database and sent
  as `httpOnly; Secure; SameSite=Strict` cookies.
- **Style**: a modular monolith. Distributed-system patterns (API gateway, database per service,
  saga, service discovery) are intentionally not used: a single free-tier instance and a single
  0.5 GB database gain nothing from them while cold starts and operational complexity would grow.
  Module boundaries are kept clean so a module can be extracted later if that ever pays off.
- **Resilience**: a circuit breaker (Resilience4j) guards the only external dependency, Google's
  token endpoint.
- **Repositories**: two repositories (frontend and backend). Infrastructure files and project
  documentation live in the backend repository under `infra/` and `docs/`.

## Consequences

- One deployable backend keeps hosting free and startup predictable.
- Shared abstractions (template-method CRUD use case, generic CRUD BLoC) remove most duplication
  across the eight near-identical financial entities.
- Moving to microservices later requires extracting modules, not rewriting them.
