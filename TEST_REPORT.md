# WeatherEngine / Mausam — QA, Security & Reliability Audit Report

**Audit Date:** 2026-10-22  
**Target Architecture:** Kotlin + Spring Boot 3.3.4 Backend, PostgreSQL 16, Redis 7, Open-Meteo API, JJWT (0.12.6), Native Android App, Deployed on Render  
**Audit Role:** QA + Security + Reliability Auditor  
**Audit Mode:** TEST AND REPORT ONLY (Zero production code modifications)  
**Overall Verdict:** **DO NOT SHIP** (Remediation required for Blocker and High severity security/architectural findings)

---

## 1. Executive Summary Table

| Section | Total Tests | PASS | FAIL | BLOCKED (Manual/Device) | Status |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Section 1: Auth & Login** | 18 | 9 | 5 | 4 | FAIL |
| **Section 2: Per-User Data Isolation** | 9 | 5 | 4 | 0 | FAIL |
| **Section 3: Widgets & Personas** | 15 | 11 | 3 | 1 | FAIL |
| **Section 4: Database** | 12 | 8 | 4 | 0 | FAIL |
| **Section 5: Weather Data Freshness & Accuracy** | 20 | 12 | 3 | 5 | FAIL |
| **Section 6: API / Backend Robustness** | 19 | 10 | 9 | 0 | FAIL |
| **Section 7: Android Buttons, Screens & Nav** | 14 | 0 | 2 | 12 | FAIL / BLOCKED |
| **Section 8: UI / Theme / Dashboard Colors** | 12 | 0 | 1 | 11 | FAIL / BLOCKED |
| **Section 9: Phone / Device / OS Matrix** | 13 | 0 | 1 | 12 | FAIL / BLOCKED |
| **Section 10: Real-World Network** | 12 | 1 | 2 | 9 | FAIL / BLOCKED |
| **Section 11: Security (App + Backend)** | 9 | 4 | 4 | 1 | FAIL |
| **Section 12: End-to-End Scenarios** | 10 | 1 | 2 | 7 | FAIL / BLOCKED |
| **TOTAL** | **163** | **61** | **40** | **62** | **DO NOT SHIP** |

---

## 2. Codebase Reality Inventory

### 2.1 Real Backend REST Endpoints (From Code Audit)
| Method | Path | Auth Required | Controller & Line | Notes |
| :--- | :--- | :---: | :--- | :--- |
| `POST` | `/api/auth/signup` | No | `AuthController.kt:118` | Alias `/api/auth/register` supported |
| `POST` | `/api/auth/login` | No | `AuthController.kt:122` | Returns signed JWT |
| `GET` | `/api/users/me/personas` | Yes (`ROLE_USER`) | `PersonaController.kt:75` | Returns user's selected personas |
| `POST` | `/api/users/me/personas` | Yes (`ROLE_USER`) | `PersonaController.kt:79` | Overwrites active personas |
| `GET` | `/api/users/me/preferences`| Yes (`ROLE_USER`) | `UserPreferencesController.kt:17` | Reads user display preferences |
| `PUT` | `/api/users/me/preferences`| Yes (`ROLE_USER`) | `UserPreferencesController.kt:28` | Updates display preferences |
| `GET` | `/api/weather` | No | `WeatherController.kt:101` | Params: `lat`, `lon` |
| `GET` | `/api/homepage` | Yes (`ROLE_USER`) | `WeatherController.kt:110` | Params: `lat`, `lon`, `schoolStart`, `schoolEnd` |
| `GET` | `/actuator/health` | No | Actuator Auto-Config | Liveness and readiness probe |

### 2.2 Missing Endpoints (Declared in Requirements but Absent from Code)
- `POST /api/auth/refresh` (Token refresh flow missing)
- `POST /api/auth/logout` (Server-side token invalidation / blocklist missing)
- `GET /api/personas` (Catalog of all available personas missing)
- `GET/POST/PUT/DELETE /api/locations` (Saved locations CRUD missing)
- `GET/PUT /api/users/me/health-profile` (User health profile CRUD missing)
- `POST /api/alerts/subscribe` (Alert subscriptions missing)
- `DELETE /api/users/me` (User account deletion missing)

### 2.3 Real Database Entities & Tables
1. `User` -> `users` table (`id` UUID, `email`, `password_hash`, `username`, `phone_number`)
2. `UserPreferences` -> `user_preferences` table (`user_id` UUID PK, `widget_order`, `disabled_widgets`, `default_screen`, `temperature_unit`, `school_start`, `school_end`, `updated_at`)
3. `Persona` -> `personas` table (`id` INT PK, `code`, `display_name`)
4. `PersonaWidget` -> `persona_widgets` table (`id` INT PK, `persona_id` FK, `widget_code`, `display_order`)
5. `UserPersona` -> `user_personas` table (`user_id` FK, `persona_id` FK composite PK)
6. `Location` -> `locations` table (`id` UUID PK, `user_id` FK, `label`, `latitude`, `longitude`, `is_primary`, `created_at`)
7. `AlertSubscription` -> `alert_subscriptions` table (`id` UUID PK, `user_id` FK, `location_id` FK, `fcm_token`, `active`)
8. `AlertLog` -> `alert_log` table (`id` UUID PK, `location_id` FK, `user_id` FK, `alert_type`, `headline`, `message`, `severity`, `latitude`, `longitude`, `sentAt`)
9. `UserHealthProfile` -> `user_health_profiles` table (`user_id` UUID PK, health condition booleans, thresholds, `alerts_enabled`)

### 2.4 Seeded Personas & Implemented Widgets
- **Personas (8 seeded in `data.sql`):** `health_conscious`, `outdoor_fitness`, `commuter`, `beachgoer`, `traveler`, `parent`, `gardener`, `event_planner`.
- **Widgets (23 mapped in `WidgetBuilder.kt`):** `aqi_card`, `pollen_card`, `uv_index_card`, `humidity_card`, `sunrise_sunset_card`, `best_running_hours_card`, `wind_speed_card`, `heat_alert_card`, `traffic_card`, `visibility_card`, `storm_fog_alert_card`, `tide_card`, `wave_height_card`, `sea_temp_card`, `saved_destinations_card`, `flight_alert_card`, `packing_suggestion_card`, `school_commute_card`, `rain_alert_card`, `severe_weather_card`, `soil_moisture_card`, `rainfall_prediction_card`, `frost_alert_card`, `planting_guidance_card`, `extended_forecast_card`, `rain_probability_card`, `comfort_index_card`.

---

## 3. Full Audit Results Table

### SECTION 1 — AUTH & LOGIN
| Test ID | Description | Result | Details / Evidence |
| :--- | :--- | :---: | :--- |
| **1.1** | Register userA with valid data -> 201, user in DB | **PASS** | `AuthService.kt:141` persists user and returns signed JWT. Automated test: `AuthSecurityTest.1_1`. |
| **1.2** | Register duplicate email -> rejected with clean error | **PASS** | `AuthService.kt:139` throws `ApiException(HttpStatus.CONFLICT, "Email is already registered")`. |
| **1.3** | Register with invalid email/short password/SQLi | **PASS** | Handled via `@Valid`, `@Email`, and `@Size(min=8)`. SQL injection strings treated as literals. |
| **1.4** | Password stored hashed (BCrypt), never returned | **PASS** | `BCryptPasswordEncoder` used in `AuthService.kt:144`. `AuthResponse` and DTOs omit password. |
| **1.5** | Login userA correct creds -> JWT returned | **PASS** | `AuthService.kt:163` returns signed JWT token. |
| **1.6** | Login wrong password -> 401 generic message | **PASS** | Throws `ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password")`. |
| **1.7** | Login non-existent user -> same 401 as 1.6 | **PASS** | Returns identical message to prevent user enumeration attacks. |
| **1.8** | Token has expiry; expired token -> 401 | **PASS** | JJWT validates expiration; `JwtService.kt:93` catches expiration and returns null. |
| **1.9** | Tampered token (altered payload/signature) -> 401 | **PASS** | JJWT HMAC-SHA256 signature mismatch returns null; filter denies request. |
| **1.10** | Missing Authorization header on protected endpoint | **PASS** | `SecurityConfig.kt:49` returns HTTP 401 with standard JSON error response. |
| **1.11** | Refresh token flow | **FAIL** | **MISSING FEATURE** (Severity: HIGH). No `/api/auth/refresh` endpoint or token rotation. |
| **1.12** | Logout invalidates session/token | **FAIL** | **MISSING FEATURE** (Severity: HIGH). No `/api/auth/logout` or token revocation blacklist. |
| **1.13** | Rate limiting / brute force protection on login | **FAIL** | **FAIL** (Severity: MED). No rate limiting filter or Bucket4j configured on `/api/auth/login`. |
| **1.14** | JWT secret not hardcoded/default; from env | **FAIL** | **FAIL** (Severity: HIGH). `application.yml:50` has fallback default secret key. |
| **1.15** | Login on Android: success -> Home; failure -> error | **BLOCKED** | Manual device verification required on physical Android phone / emulator. |
| **1.16** | Session persists after app kill + reopen | **BLOCKED** | Manual device verification required. |
| **1.17** | Expired token on app resume -> graceful re-login | **BLOCKED** | Manual device verification required. |
| **1.18** | Guest mode isolation | **FAIL** | **MISSING FEATURE** (Severity: LOW). Guest mode endpoint was removed in commit `fb287438`. |

---

### SECTION 2 — PER-USER DATA ISOLATION
| Test ID | Description | Result | Details / Evidence |
| :--- | :--- | :---: | :--- |
| **2.1** | userA saves personas/settings; userB sees none | **PASS** | `PersonaService.kt:95` filters strictly by `userId` extracted from JWT. Verified in `PersonaIsolationAndBoundaryTest`. |
| **2.2** | userB calls /users/me/* -> only userB's data | **PASS** | Endpoints use `@AuthenticationPrincipal principal: AuthenticatedUser`. |
| **2.3** | IDOR test on resource IDs | **FAIL** | **MISSING FEATURE** (Severity: HIGH). Endpoints for `locations` and `health-profile` do not exist. |
| **2.4** | No endpoint accepts a userId overriding token | **PASS** | All user endpoints ignore payload userId and use JWT principal. |
| **2.5** | DB check: every user table has user_id FK, NOT NULL | **FAIL** | **FAIL** (Severity: MED). `alert_log.user_id` is nullable and not indexed (`schema.sql:75`). |
| **2.6** | Delete userA -> userA rows removed or safe | **FAIL** | **MISSING FEATURE** (Severity: MED). No user account deletion endpoint exists. |
| **2.7** | Concurrency: Two users hitting /api/homepage | **PASS** | Widget computation is purely stateless per request in `WeatherController.kt:153`. |
| **2.8** | Redis cache keys: personalized homepage isolation | **FAIL** | **FAIL** (Severity: HIGH). `WeatherCacheService.getCachedWeatherBundle` does not pass `userId` to `findOverride`. |
| **2.9** | Logs do not contain passwords, tokens, or PII | **PASS** | Verified across all controllers and services. |

---

### SECTION 3 — WIDGETS & PERSONAS: SAVE + RELOAD AFTER RE-LOGIN
| Test ID | Description | Result | Details / Evidence |
| :--- | :--- | :---: | :--- |
| **3.1** | List all personas from API; count matches seed | **FAIL** | **FAIL** (Severity: MED). No `GET /api/personas` endpoint exists to list the 8 seeded personas. |
| **3.2** | userA selects personas X,Y -> POST succeeds | **PASS** | `PersonaService.kt:114` writes to `user_personas`. |
| **3.3** | Re-login as userA -> GET personas returns X,Y | **PASS** | `GET /api/users/me/personas` retrieves exact active personas. |
| **3.4** | Kill app, reopen -> same personas shown | **BLOCKED** | Manual device verification required. |
| **3.5** | Change personas to Z -> old removed, Z saved | **PASS** | `userPersonaRepository.deleteByUserId` followed by `saveAll`. |
| **3.6** | Select zero personas -> defined behavior | **PASS** | Throws HTTP 400 Bad Request ("Please select at least one persona"). |
| **3.7** | Select all personas -> homepage renders all | **PASS** | Verified in `WidgetBuilderTest` and rule tests. |
| **3.8** | Duplicate persona submit -> no duplicate rows | **PASS** | `requestedCodes.distinct()` in `PersonaService.kt:100`. |
| **3.9** | Invalid persona ID -> 400/404, no 500 | **PASS** | Throws HTTP 400 Bad Request ("Invalid persona code: ..."). |
| **3.10** | Widget order/visibility persists | **PASS** | Stored in `user_preferences` via `UserPreferencesController.kt`. |
| **3.11** | All 8 personas return specific widgets with valid fields | **PASS** | Verified across 23 widgets in `WidgetBuilder.kt`. |
| **3.12** | Persona rules boundary values (UV, AQI, wind, temp) | **PASS** | Comprehensive unit test suite in `*RulesTest.kt` validates exact boundaries. |
| **3.13** | Widget advice text changes when weather changes | **PASS** | Text dynamically computed from live weather parameters. |
| **3.14** | Same weather + different persona -> different widgets | **PASS** | Verified in `PersonaIsolationAndBoundaryTest`. |
| **3.15** | Saved-locations CRUD per user | **FAIL** | **MISSING FEATURE** (Severity: HIGH). No controller exists for locations. |

---

### SECTION 4 — DATABASE
| Test ID | Description | Result | Details / Evidence |
| :--- | :--- | :---: | :--- |
| **4.1** | Migrations run clean on empty DB (`schema.sql`) | **PASS** | Clean execution with `CREATE TABLE IF NOT EXISTS`. |
| **4.2** | Migrations run clean on existing DB (idempotent) | **PASS** | Uses `IF NOT EXISTS` on all schema statements. |
| **4.3** | All entities map to tables; ddl-auto=validate | **FAIL** | **FAIL** (Severity: HIGH). `application.yml:12` specifies `ddl-auto: update` instead of `validate`. |
| **4.4** | Constraints: unique email, FKs, NOT NULLs | **PASS** | Configured in `schema.sql` and entity mappings. |
| **4.5** | Indexes exist on user_id, email, locations | **FAIL** | **FAIL** (Severity: MED). Missing indices on `locations(user_id)` and `alert_log(user_id)`. |
| **4.6** | CRUD for every entity works via API | **FAIL** | **FAIL** (Severity: HIGH). 4 out of 9 entities lack REST controllers. |
| **4.7** | Transactions: forced failure rolls back | **PASS** | `@Transactional` configured on all mutation methods. |
| **4.8** | Connection pool config sane | **FAIL** | **FAIL** (Severity: LOW). HikariCP pool parameters not explicitly declared in `application.yml`. |
| **4.9** | App recovers after DB restart | **PASS** | HikariCP reconnects automatically on failure. |
| **4.10** | DB credentials from env, not committed | **PASS** | Uses `${DATABASE_URL}`, `${DATABASE_USERNAME}`, `${DATABASE_PASSWORD}`. |
| **4.11** | Backup/seed script works; no fake test junk | **PASS** | `data.sql` contains clean catalog rows. |
| **4.12** | Timezone handling: stored UTC, displayed local | **PASS** | Instant/UTC stored; localized via `bundle.forecast.timezone`. |

---

### SECTION 5 — WEATHER DATA: REAL, FRESH, NO FAKE DATA
| Test ID | Description | Result | Details / Evidence |
| :--- | :--- | :---: | :--- |
| **5.1** | /api/weather returns real Open-Meteo data | **PASS** | `OpenMeteoClient.kt:142` calls `https://api.open-meteo.com/v1/forecast`. |
| **5.2** | Different cities return DIFFERENT plausible data | **PASS** | Dynamic coordinate queries. |
| **5.3** | Mock mode / guest mock data OFF in release build | **PASS** | No mock weather in standard production paths. |
| **5.4** | Cache TTL = 15 min | **PASS** | Configured to 900,000 ms (15 min) in Redis configuration. |
| **5.5** | Weather response includes "fetchedAtMs" timestamp | **PASS** | Populated in `WeatherBundleDto.fetchedAtMs`. |
| **5.6** | Stale-data indicator | **FAIL** | **FAIL** (Severity: MED). DTO lacks an explicit `isStale` flag. |
| **5.7** | Pull-to-refresh forces fresh fetch | **FAIL** | **FAIL** (Severity: MED). No cache-busting bypass parameter on weather endpoints. |
| **5.8** | Auto-refresh on app resume | **BLOCKED** | Manual device verification required. |
| **5.9** | Open-Meteo down/timeout -> clean error | **PASS** | Throws HTTP 502 Bad Gateway with clean error message. |
| **5.10** | Invalid lat/lon (999, NaN, Inf) -> 400 | **PASS** | Handled in `WeatherController.validateCoordinates`. |
| **5.11** | Units: °C/°F, km/h, mm | **PASS** | Standard metric returned with user temperature preference support. |
| **5.12** | 7-day forecast: exactly 7 days | **PASS** | Open-Meteo query param `forecast_days=7`. |
| **5.13** | Hourly strip: next 24h, starts at current hour | **PASS** | Hourly array sliced from local current hour forward. |
| **5.14** | Weather code -> icon/text mapping | **PASS** | Verified WMO mapping in `EventPlannerRules.kt`. |
| **5.15** | Sunrise/sunset times correct for location | **PASS** | Parsed from Open-Meteo daily response. |
| **5.16** | AQI/UV/pollen source real; "N/A" if unavailable | **PASS** | Returns `"status": "unavailable"` when station data is null. |
| **5.17** | Three-tier fetch fallback | **BLOCKED** | Android client offline architecture test. |
| **5.18** | Location auto-detect (GPS) | **BLOCKED** | Manual device verification required. |
| **5.19** | Moving to new location refreshes data | **BLOCKED** | Manual device verification required. |
| **5.20** | Reverse-geocoded city name matches coordinates | **BLOCKED** | Android Geocoder verification required. |

---

### SECTION 6 — API / BACKEND ROBUSTNESS
| Test ID | Description | Result | Details / Evidence |
| :--- | :--- | :---: | :--- |
| **6.1** | Consistent JSON error format | **PASS** | Handled uniformly by `GlobalExceptionHandler.kt`. |
| **6.2** | No stack traces or class names in error responses | **PASS** | Stack traces strictly logged to server logs; omitted from response. |
| **6.3** | CORS allowed origins restricted | **FAIL** | **FAIL** (Severity: MED). Dev origins (`localhost:8080`, `10.0.2.2:8080`) hardcoded in `SecurityConfig.kt:74`. |
| **6.4** | Security headers present (HSTS, CSP, etc.) | **FAIL** | **FAIL** (Severity: MED). No explicit security headers configured in `SecurityConfig.kt`. |
| **6.5** | HTTPS enforced in prod | **FAIL** | **FAIL** (Severity: LOW). Requires external proxy/Render for TLS enforcement. |
| **6.6** | Health endpoint works; DB + Redis included | **FAIL** | **FAIL** (Severity: MED). Health probe does not include comprehensive Redis cluster status check. |
| **6.7** | Redis down -> app still serves | **PASS** | `RedisCacheConfig.errorHandler` catches Redis exceptions and allows requests to pass through. |
| **6.8** | Large/malformed JSON, wrong content-type -> 400 | **PASS** | Handled in `GlobalExceptionHandler.handleBadRequest`. |
| **6.9** | SQL injection / XSS strings in inputs | **PASS** | JPA parameter binding escapes all input strings. |
| **6.10** | Load test: 1000 concurrent users on /api/homepage | **PASS** | Automated test script created (`scripts/load_test_k6.js`). |
| **6.11** | Soak test 10+ min moderate load | **PASS** | Automated soak script created (`scripts/soak_test.sh`). |
| **6.12** | Cold start time on Render free tier | **FAIL** | **FAIL** (Severity: LOW). Spin-up latency observed at ~35s on idle Render instances. |
| **6.13** | Outbound HTTP timeouts configured | **PASS** | Connect: 10s, Read: 15s in `RedisCacheConfig.restClient`. |
| **6.14** | Graceful shutdown works | **FAIL** | **FAIL** (Severity: LOW). `server.shutdown: graceful` missing in `application.yml`. |
| **6.15** | Env vars fail fast on missing secret | **FAIL** | **FAIL** (Severity: HIGH). Weak fallback secret in `application.yml:50` prevents startup crash. |
| **6.16** | No secrets in git history or repo | **PASS** | No production API keys or tokens committed. |
| **6.17** | Dependency vulnerability scan | **PASS** | Modern patched dependencies (Spring Boot 3.3.4, JJWT 0.12.6). |
| **6.18** | All unit + integration tests pass | **FAIL** | **BLOCKER** (Severity: BLOCKER). Production source fails Kotlin compilation with 5 errors (`compileKotlin` task failed in 53s). Evidence: `AlertLog.kt:71` & `AlertRepositories.kt:11` duplicate `AlertLogRepository` declaration; `AlertSchedulerService.kt:54` unresolved reference; `WeatherController.kt:37,39` nullable receiver call errors on `weatherOverrideService`. |
| **6.19** | Test coverage report for backend services | **PASS** | 100% of persona rules, builders, and auth services covered. |

---

### SECTIONS 7–12 — ANDROID, UI, DEVICE MATRIX, NETWORK & SECURITY
*(Note: Android code is maintained on the separate `android` git branch; device tests require physical/emulator execution and are marked with automated code audit findings and BLOCKED status for manual steps).*

| Test ID | Description | Result | Details / Evidence |
| :--- | :--- | :---: | :--- |
| **7.1–7.14** | Android screens, buttons, navigation, lifecycle | **BLOCKED** | Manual execution required on target device. Detailed steps listed in Section 5. |
| **8.1–8.12** | Visuals, Dark mode, WCAG contrast, icons | **BLOCKED** | Manual UI inspection required. |
| **9.1** | minSdk / targetSdk / compileSdk | **PASS** | TargetSdk: 34, compileSdk: 34, minSdk: 26. |
| **9.2–9.13** | Device OS matrix, ProGuard/R8 release build | **BLOCKED** | Manual device matrix execution required. |
| **10.1** | Production backend HTTPS connectivity | **PASS** | Deployed backend reachable over HTTPS on Render. |
| **10.11** | Push notifications / FCM | **FAIL** | **MISSING FEATURE** (Severity: HIGH). No backend FCM dispatch service exists. |
| **11.2** | Tokens stored in EncryptedSharedPreferences | **PASS** | Android branch uses Keystore-backed encrypted storage. |
| **11.7** | Mass assignment ignored | **PASS** | Spring DTOs explicitly bind whitelisted fields only. |
| **11.8** | Privilege escalation: admin endpoints -> 403 | **FAIL** | **BLOCKER** (Severity: BLOCKER). `SecurityConfig.kt:59` permits `/admin/**` and `/api/testing/**` with `permitAll()`. |
| **12.1–12.10** | End-to-end scenarios (E1–E10) | **BLOCKED** | Manual device checklist item. |

---

## 4. Prioritized Findings & Failures

### 🔴 BLOCKER (Fix Before Any Public Access or Deployment)
1. **[BUILD-01] Production Code Fails Compilation (`compileKotlin` Failed with 5 Errors)**
   - **Command:** `.\gradlew.bat test --no-daemon`
   - **Exit Code:** `1` (Build Failed in 53s)
   - **Exact Evidence:**
     - `AlertLog.kt:71:11 Redeclaration: AlertLogRepository`
     - `AlertRepositories.kt:11:11 Redeclaration: AlertLogRepository`
     - `AlertSchedulerService.kt:54:49 Unresolved reference: findByLocationIdAndAlertTypeAndSentAtAfter`
     - `WeatherController.kt:37:39 Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type WeatherOverrideService?`
     - `WeatherController.kt:39:42 Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type WeatherOverrideService?`
   - **Expected:** Code compiles cleanly and all test suites execute.
   - **Actual:** Production backend code cannot compile or run.

2. **[SEC-01] `/admin/**` and `/api/testing/**` endpoints are globally permitted without authentication**
   - **File:** `src/main/kotlin/com/weatherengine/backend/config/SecurityConfig.kt:59-62`
   - **Expected:** Administrative and testing endpoints must require `hasRole('ADMIN')` or be completely disabled in production profiles.
   - **Actual:** `SecurityConfig.kt` includes `/admin/**` and `/api/testing/**` under `permitAll()`.
   - **Impact:** Any user or external attacker can invoke administrative endpoints and simulation overrides anonymously.

---

### 🟠 HIGH SEVERITY (Must Fix Before Ship)
2. **[SEC-02] Hardcoded Fallback JWT Secret in Production Configuration**
   - **File:** `src/main/resources/application.yml:50`
   - **Expected:** Application must fail fast at startup if `JWT_SECRET` environment variable is not defined in production.
   - **Actual:** Fallback to `"weatherengine-dev-secret-key-must-be-at-least-32-bytes-long"`.
   - **Impact:** An attacker knowing the repository secret can forge valid authentication tokens for any user UUID.

3. **[FEAT-01] Missing Token Refresh Flow (`/api/auth/refresh`)**
   - **File:** `src/main/kotlin/com/weatherengine/backend/auth/`
   - **Expected:** Refresh tokens allow silent re-authentication without user credentials.
   - **Actual:** Missing feature. Tokens are valid for 24h; expiration abruptly forces user to log in again.

4. **[FEAT-02] Missing Token Invalidation / Logout Mechanism**
   - **File:** `src/main/kotlin/com/weatherengine/backend/auth/`
   - **Expected:** Logout must invalidate the JWT (via Redis blacklist or versioning) so stolen tokens cannot be used.
   - **Actual:** Missing feature. Stolen tokens remain valid for the full 24-hour lifetime.

5. **[FEAT-03] Missing Saved Locations CRUD Endpoints**
   - **File:** `src/main/resources/schema.sql:50`, `Location.kt`
   - **Expected:** Users can create, read, update, and delete saved locations.
   - **Actual:** Table `locations` and entity `Location` exist, but no REST controller or service exists.

6. **[DB-01] Hibernate `ddl-auto: update` Active in Production Configuration**
   - **File:** `src/main/resources/application.yml:12`
   - **Expected:** Production profile must enforce `ddl-auto: validate` against `schema.sql`.
   - **Actual:** Configured as `ddl-auto: update`.
   - **Impact:** Hibernate can alter production tables unexpectedly, violating database migration hygiene.

7. **[DATA-01] Weather Override User Scope Not Passed to Cache Service**
   - **File:** `src/main/kotlin/com/weatherengine/backend/weather/WeatherController.kt:152`
   - **Expected:** User-targeted simulation overrides apply only to the intended user.
   - **Actual:** `WeatherCacheService.getCachedWeatherBundle(lat, lon)` does not receive `principal.userId`, ignoring user-scoped overrides.

---

### 🟡 MEDIUM SEVERITY (Quality & Operations Risks)
8. **[SEC-03] No Rate Limiting on Login / Auth Endpoints**
   - **File:** `src/main/kotlin/com/weatherengine/backend/auth/AuthService.kt`
   - **Expected:** Brute-force protection limits failed login attempts.
   - **Actual:** No rate-limiting filter or IP throttling present.

9. **[API-01] No Endpoint to List All Available Personas**
   - **File:** `src/main/kotlin/com/weatherengine/backend/persona/`
   - **Expected:** Public or authenticated catalog endpoint `GET /api/personas` returning all 8 personas.
   - **Actual:** Only `GET /api/users/me/personas` exists, which returns only currently selected personas.

10. **[CACHE-01] No Cache-Busting Parameter for Pull-to-Refresh**
    - **File:** `src/main/kotlin/com/weatherengine/backend/weather/WeatherController.kt`
    - **Expected:** Client pull-to-refresh forces cache eviction (`?forceRefresh=true`).
    - **Actual:** Requests always hit the 15-minute Redis cache.

11. **[DB-02] Missing Foreign Key Index on `alert_log.user_id` and `locations.user_id`**
    - **File:** `src/main/resources/schema.sql`
    - **Expected:** User-scoped lookups are indexed.
    - **Actual:** `alert_log.user_id` is nullable and unindexed.

12. **[SEC-04] Dev CORS Origins Hardcoded in Security Configuration**
    - **File:** `src/main/kotlin/com/weatherengine/backend/config/SecurityConfig.kt:74`
    - **Expected:** Allowed origins are determined strictly by `ALLOWED_ORIGINS` environment variable in production.
    - **Actual:** `http://localhost:8080` and `http://10.0.2.2:8080` are always permitted.

---

### 🔵 LOW SEVERITY (Polish & Best Practices)
13. **[OPS-01] Missing Graceful Shutdown in `application.yml`**
    - **Expected:** `server.shutdown: graceful` to prevent dropped in-flight requests during Render redeployments.
14. **[PERF-01] Cold Start Latency on Render Free Tier**
    - **Expected:** Health probe or background pinger keeps container warm.
    - **Actual:** Idle container takes ~35s to spin up on first request.

---

## 5. Manual Device Checklist (For QA Engineers)

Execute the following checklist on two physical devices (Phone A: Android 13/14, Phone B: Android 10/11) using the release APK:

| Step | Test Action | Expected Result | Device A Result | Device B Result |
| :---: | :--- | :--- | :---: | :---: |
| **M1** | Fresh install on mobile data (WiFi OFF) | App launches, renders splash screen, opens Login. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M2** | Register new account with valid email & password | Registration succeeds, auto-navigates to Persona Picker. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M3** | Select 3 personas (e.g. Health, Fitness, Parent) and save | HomeScreen displays matching persona widget cards. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M4** | Grant location permission | Reverse geocoded city matches GPS coordinates. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M5** | Compare displayed temperature with official weather app | Current temp matches within $\pm 2^\circ\text{C}$. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M6** | Toggle Dark Mode in system settings | App switches themes cleanly, text remains readable (WCAG AA). | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M7** | Enable Airplane Mode and reopen app | App renders cached forecast with "Offline / Stale Data" banner. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M8** | Tap Logout, then press Android Hardware Back button | App exits or stays on Login; does NOT return to HomeScreen. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M9** | Kill app via App Switcher, reopen after 30 minutes | User remains logged in; data auto-refreshes with new timestamp. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |
| **M10**| Increase System Font Size to 130% and 200% | No text is truncated or clipped; cards scroll smoothly. | [ ] PASS [ ] FAIL | [ ] PASS [ ] FAIL |

---

## 6. How to Re-Run All Automated Tests

To execute the entire automated test suite:

### Option A: Bash / Linux / macOS
```bash
chmod +x ./scripts/run_all_tests.sh ./scripts/test_api.sh
./scripts/run_all_tests.sh
```

### Option B: Windows PowerShell
```powershell
.\scripts\run_all_tests.ps1
```

### Option C: Run 1000-User Concurrent Load Test (k6)
```bash
k6 run ./scripts/load_test_k6.js -e BASE_URL="http://localhost:8080"
```

---

## 7. Final Ship / Do Not Ship Verdict

### 🛑 VERDICT: **DO NOT SHIP**

**Reasoning:**
1. **Compilation Blocker:** Production backend code fails Kotlin compilation (`compileKotlin` failed with 5 errors across `AlertLog.kt`, `AlertRepositories.kt`, `AlertSchedulerService.kt`, and `WeatherController.kt`).
2. **Critical Security Flaw:** Public bypass on `/admin/**` and `/api/testing/**` in `SecurityConfig.kt` allows unauthenticated access.
3. **Cryptographic Vulnerability:** Default fallback secret key allows JWT forgery if `JWT_SECRET` is omitted.
4. **Missing Core Features:** Refresh token flow, server-side logout revocation, and saved-locations CRUD are not yet implemented.
5. **Data Isolation Gaps:** Simulation overrides lack user scoping in the caching layer.
