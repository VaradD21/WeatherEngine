# AGENTS.md

## Project Overview
- **Project**: Weather Engine Backend
- **Platform**: Spring Boot 3.3.4, Kotlin 1.9.25, JDK 17+
- **Package Root**: `com.weatherengine.backend`
- **Architecture**: Stateless REST API with Spring Security

## Coding & Architectural Conventions
1. **File Length**: Keep files concise (< 300 lines). Break components into clean modules.
2. **Types**: Use idiomatic Kotlin types; avoid `Any` unless strictly necessary.
3. **Immutability & Null-Safety**: Prefer `val` over `var` and leverage Kotlin's null-safety features.
4. **Configuration**: Use `@ConfigurationProperties` or idiomatic Spring configuration. Reuse existing application properties; do not invent redundant property names.
5. **Security**:
   - Stateless session management (`SessionCreationPolicy.STATELESS`).
   - CSRF disabled for stateless JWT REST APIs.
   - Public access strictly limited to designated paths (`/api/auth/**`, `/actuator/health`, `/error`).
   - Explicit origin matching for CORS; never use wildcard origins with credentials enabled.
   - Deny by default for unmatched routes.
6. **Commands**: Never run terminal commands automatically without user approval.
