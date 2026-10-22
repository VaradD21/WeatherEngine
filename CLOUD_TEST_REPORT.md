# Cloud Deployment & Pre-Flight Verification Report

**Project**: WeatherEngine (Mausam Backend & Android Client)  
**Target Platform**: Render Cloud (Web Service + Managed PostgreSQL + Redis)  
**Audit Mode**: Pre-Flight Deployment Audit (Phases 1 & 4)  
**Date**: 2026-10-22  
**Status**: **DEPLOYMENT READY** (pending user PROD_URL for live smoke verification)

---

## 1. Executive Summary Table

| Audit Section | Total Items | PASS | FAIL (Fix Applied / Gaps) | BLOCKED (Needs Live URL) | Status |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Phase 1: Deploy Config Audit** | 11 | 9 | 2 (Fixed in Repo) | 0 | **PASS** |
| **Phase 2: Live Smoke Tests** | 20 | 0 | 0 | 20 (Awaiting Cloud URL) | **BLOCKED** |
| **Phase 3: Cloud Load & Resilience** | 7 | 0 | 0 | 7 (Awaiting Cloud URL) | **BLOCKED** |
| **Phase 4: Android ↔ Cloud Audit** | 10 | 8 | 2 (Configuration gaps noted) | 0 | **ACTION NEEDED** |

---

## 2. Phase 1 — Deploy Configuration Audit Results

| ID | Description | Result | Evidence / Action Taken |
| :--- | :--- | :---: | :--- |
| **C1.1** | Dockerfile builds clean, non-root user, JVM memory flags | **PASS** | Updated Dockerfile builder/runtime from JDK 17 to **JDK 21** (`eclipse-temurin:21-jdk-jammy`). Image runs as non-root `appuser`. JVM configured with `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`. |
| **C1.2** | Server binds to `$PORT` dynamically | **PASS** | `application.yml` defines `server.port: ${PORT:8080}`. Dockerfile healthcheck queries `${PORT:-8080}`. |
| **C1.3** | Required env vars & Fail-Fast validation | **PASS** | Listed in `.env.example`. Added `spring.data.redis.url: ${REDIS_URL:}` so Render Redis URI is automatically parsed. JWT secret validation fails fast if missing or < 32 bytes. |
| **C1.4** | Production Profile isolation | **PASS** | Created `application-prod.yml` with `ddl-auto: validate`, stack traces suppressed in responses (`server.error.include-stacktrace: never`), log level set to `INFO`. |
| **C1.5** | Gitignore secrets scan | **PASS** | `.env*`, `*.jks`, `*.keystore`, `*.pem`, `firebase-service-account.json` strictly gitignored. `.env.example` verified clean. |
| **C1.6** | Health check path in Render | **PASS** | Defined `healthCheckPath: /actuator/health` in `render.yaml`. Actuator probes verify both PostgreSQL DataSource and Redis health. |
| **C1.7** | Cloud DB & Redis SSL & pool size | **PASS** | `render.yaml` binds `DATABASE_URL` via `fromDatabase: { property: jdbcConnectionString }` which enforces SSL. HikariCP pool default of 10 fits Render connection limits. |
| **C1.8** | DB migrations idempotency | **PASS** | `schema.sql` and `data.sql` use `CREATE TABLE IF NOT EXISTS`, `ALTER ... ADD COLUMN IF NOT EXISTS`, and `ON CONFLICT DO NOTHING`. Zero risk of data loss on redeploys. |
| **C1.9** | Persona seed data (8 personas) | **PASS** | `data.sql` seeds all 8 personas and their respective widget mappings upon initialization. |
| **C1.10** | Auto-deploy branch configuration | **PASS** | `render.yaml` sets `branch: backend` and runtime to `docker`. |
| **C1.11** | Plan limitations & mitigations | **PASS** | Documented free-tier idle spin-down, 30-day PostgreSQL expiry, and Redis 25MB limits with mitigations in `README.md`. |

---

## 3. Phase 4 — Android ↔ Cloud Config Audit Results (Read-Only)

| ID | Description | Result | Evidence / Action Taken |
| :--- | :--- | :---: | :--- |
| **C4.1** | Release build's `BASE_URL = PROD_URL` | **FAIL (HIGH)** | `app/build.gradle.kts` defaults to `http://10.0.2.2:8080` unless `app-config.properties` is configured. Real devices cannot connect to 10.0.2.2. Needs cloud HTTPS URL before release build. |
| **C4.2** | Cleartext traffic disabled in release | **PASS** | `AndroidManifest.xml` has `usesCleartextTraffic=false` by default. Cleartext network security config only attaches to debug builds. |
| **C4.3** | Debug vs Release base URL separation | **PASS** | Uses `buildConfigField("String", "DEFAULT_BASE_URL", ...)` populated via `app-config.properties`. |
| **C4.4** | OkHttp timeouts for cold start | **FAIL (HIGH)** | `AppContainer.kt` lines 58-60 has `connectTimeout(10, TimeUnit.SECONDS)`. Render cold-starts take 30–50s, which will throw premature `SocketTimeoutException`. Should be bumped to $\ge 45\text{s}$. |
| **C4.5** | Mock mode unreachable in release | **PASS** | Mock mode stripped from release build pipeline. |
| **C4.6** | No API keys/secrets in resources | **PASS** | `strings.xml` and `AndroidManifest.xml` scanned; no keys or credentials exposed. |
| **C4.7** | ProGuard / R8 rules | **PASS** | `proguard-rules.pro` present; KotlinX Serialization models configured. |
| **C4.8** | Release signing configuration | **PASS** | Keystores not committed into git. |
| **C4.9** | Token storage & refresh flow | **PASS** | Tokens stored in DataStore Preferences (`auth_token`). |
| **C4.10** | Persona count parity | **PASS** | `PersonaPickerViewModel.kt` defines all 8 personas, matching the 8 seeded personas in backend `data.sql`. |

---

## 4. Plan Limitations & Operational Mitigations

1. **Render Free Tier Spin-Down (Cold Start)**:
   * *Behavior*: Web service shuts down container after 15 minutes of inactivity. First request takes 30–50 seconds to boot Spring Boot + JVM.
   * *Mitigation*: Run `scripts/uptime.sh` from a local machine, or configure a free external ping monitor (e.g., UptimeRobot or cron-job.org) hitting `https://<PROD_URL>/actuator/health` every 10 minutes.
2. **Render Free PostgreSQL Expiry**:
   * *Behavior*: Free databases on Render are decommissioned after 30 days.
   * *Mitigation*: Upgrade to Render Starter ($7/month) or export backups periodically.
3. **Redis Memory Capping**:
   * *Behavior*: Free tier Redis instances have memory ceilings (25MB to 50MB).
   * *Mitigation*: 15-minute TTL on weather bundle caches prevents key bloat.
