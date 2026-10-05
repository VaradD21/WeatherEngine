# WeatherEngine

Monorepo workspace containing the **WeatherEngine Backend API** and **WeatherEngine Android App**.

## Modules
- **[`weatherengine-backend`](./weatherengine-backend/README.md)** (`backend` branch): Kotlin + Spring Boot 3 REST API with PostgreSQL, Redis caching, JWT auth, and persona widget aggregation.
- **[`weatherengine-android`](./weatherengine-android/README.md)** (`android` branch): Native Android client built with Kotlin, Jetpack Compose (Material 3), 3-tier weather fallback, and on-device persona rules.

## Local Docker Quickstart (Backend + Postgres + Redis)
```powershell
docker compose up --build
```
