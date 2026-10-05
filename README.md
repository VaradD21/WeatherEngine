# WeatherEngine Backend API

Production-ready Spring Boot 3 (Kotlin) backend for **WeatherEngine**, providing JWT authentication, persona management, Redis-cached Open-Meteo weather aggregation, and personalized weather widget computation.

---

## Tech Stack
- **Language & Runtime**: Kotlin 1.9.25, Java 17
- **Framework**: Spring Boot 3.3.4 (Web, Security, Data JPA, Validation, Cache, Actuator)
- **Database**: PostgreSQL 16 (schema managed via `schema.sql` and seeded via `data.sql`)
- **Cache**: Redis 7 (`weather-bundle` cache with 15-minute TTL)
- **Authentication**: Stateless JWT (`jjwt` 0.12.6) + BCrypt password hashing
- **Testing**: JUnit 5 + MockK

---

## Project Structure

```text
src/main/kotlin/com/weatherengine/backend/
├── BackendApplication.kt          # Spring Boot entry point
├── auth/
│   ├── AuthController.kt          # POST /api/auth/signup, POST /api/auth/login + Auth DTOs
│   ├── AuthService.kt             # User registration, BCrypt verification, default persona assignment
│   └── JwtService.kt              # JWT signing/verification + JwtAuthenticationFilter
├── common/
│   └── GlobalExceptionHandler.kt  # @RestControllerAdvice + ApiException
├── config/
│   ├── RedisCacheConfig.kt        # Redis cache manager (15-min TTL) + RestClient bean
│   └── SecurityConfig.kt          # Stateless Spring Security filter chain + CORS + 401 EntryPoint
├── persona/
│   ├── Persona.kt                 # Persona, PersonaWidget, UserPersona entities, DTOs & repositories
│   └── PersonaService.kt          # Persona CRUD service + GET/POST /api/users/me/personas controller
├── user/
│   └── User.kt                    # User JPA entity + UserRepository
└── weather/
    ├── OpenMeteoClient.kt         # Upstream Open-Meteo Forecast & Air Quality HTTP client
    ├── OpenMeteoModels.kt         # Upstream response DTOs + WeatherBundleDto & WidgetDto
    ├── WeatherController.kt       # GET /api/weather, GET /api/homepage + WeatherCacheService
    └── WidgetBuilder.kt           # Computes persona-specific widget payloads from WeatherBundleDto
```

---

## REST API Endpoints

### Public Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/signup` | Register a new user (`email`, `password`) and return a signed JWT |
| `POST` | `/api/auth/login` | Authenticate an existing user and return a signed JWT |
| `GET` | `/api/weather?lat={lat}&lon={lon}` | Fetch Redis-cached 7-day forecast + air quality bundle for coordinates |
| `GET` | `/actuator/health` | Liveness and readiness health probe |

### Authenticated Endpoints (`Authorization: Bearer <token>`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/users/me/personas` | List active personas for the authenticated user |
| `POST` | `/api/users/me/personas` | Replace active personas (`{"personaCodes": ["health_conscious", ...]}`) |
| `GET` | `/api/homepage?lat={lat}&lon={lon}` | Compute personalized weather widgets for the user's selected personas |

---

## Environment Variables

| Variable | Default (Dev) | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | HTTP server port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/weatherengine` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | `postgres` | PostgreSQL username |
| `DATABASE_PASSWORD` | `postgres` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | *(empty)* | Redis password (optional) |
| `SPRING_CACHE_TYPE` | `redis` | Cache provider (`redis` or `none`) |
| `JWT_SECRET` | *(dev fallback)* | HMAC-SHA256 secret key (must be $\ge 32$ bytes in production) |
| `JWT_EXPIRATION_MS` | `86400000` | JWT validity duration in milliseconds (24 hours) |
| `ALLOWED_ORIGINS` | `http://localhost:8080,http://10.0.2.2:8080` | Comma-separated CORS allowed origins |

---

## Running Locally

### 1. Run Unit Tests
```powershell
.\gradlew.bat test
```

### 2. Start Backend Server
```powershell
.\gradlew.bat bootRun
```

### 3. Build Production Docker Image
```powershell
docker build -t weatherengine-backend .
```
