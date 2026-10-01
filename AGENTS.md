# WeatherEngine Backend — Agent Rules

- Stack: Kotlin + Spring Boot, PostgreSQL, Redis, JWT auth. Package root: `com.weatherengine.backend`.
- Package-by-feature, not layer: `auth/`, `persona/`, `weather/`, `alert/`, `location/`, `user/`, `config/`, `common/`.
- The app is API-only. It never renders UI. The Android app is a separate consumer.
- The app never calls third-party weather APIs from the client — all weather/AQI/UV calls go through this backend's `weather` module, which normalizes and caches (Redis, 15 min TTL) before returning to the client.
- Persona → widget mapping lives server-side (`persona_widgets` table), seeded in `data.sql`. Don't hardcode persona logic in the Android app or invent new widget codes without updating `data.sql`.
- `schema.sql` is the source of truth for the DB schema. `ddl-auto: validate` — Hibernate never auto-generates schema. Any entity change must be reflected in `schema.sql` first.
- Auth/JWT/security code: full validation always, no shortcuts, no ponytail minimalism applied here even if ponytail is active elsewhere in the session.
- Alerts are push-based via FCM from a scheduled backend job (`@Scheduled`), not polled by the client. Dedup sent alerts using `alert_log` before sending.
- Write JUnit5 + MockK tests for new service-layer methods. Integration tests use Testcontainers Postgres, not an in-memory DB substitute.
- Never commit real API keys or secrets — use `.env` (gitignored), reference `.env.example` for required vars.
