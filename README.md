# Atlas — User Service

> Owns user profile and preferences.

Part of **[Atlas](https://github.com/atlas-event-lab)**.

## Responsibilities

- Store and serve the authenticated user's profile and preferences.
- Identity itself is handled by Keycloak; this service consumes the JWT, it never issues one.

## Tech

Java 21 · Spring Boot · Spring Data JPA · PostgreSQL (`user_db`) · Keycloak JWT.
REST-only — no Kafka in the MVP.

## API

| Method | Path | Description |
|--------|------|-------------|
| GET · POST | `/api/v1/me/profile` | Read / upsert the current user's profile |

The `UserId` is extracted from the JWT; client-supplied identifiers are never trusted.

## Events

None (REST-only).

## Data

Owns `user_db` (database-per-service).

## Run locally

```bash
docker compose up user-service
```

Env: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KEYCLOAK_ISSUER_URI`.

## License

Apache-2.0 — see [`LICENSE`](./LICENSE).
