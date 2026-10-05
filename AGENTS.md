# WeatherEngine Android App

## Guidelines
- **Architecture**: MVVM with Jetpack Compose (Material 3) and package-by-feature layout.
- **Dependency Injection**: Manual wiring through `AppContainer` initialized in `WeatherEngineApp` (`MainActivity.kt`).
- **3-Tier Weather Data Flow**:
  - Tier 1: Backend `/api/weather` proxy (Redis-cached bundle).
  - Tier 2: Direct Open-Meteo Forecast + Air Quality API fallback when backend is unreachable.
  - Tier 3: Local Jetpack DataStore offline cache (`SettingsStore`).
- **Security & Privacy**:
  - Never log tokens, passwords, authorization headers, or request/response bodies containing user credentials.
  - Cleartext HTTP traffic is only permitted in debug builds for local emulator testing via `network_security_config.xml`. Release builds strictly require HTTPS.
- **File Limits**: Keep files modular and concise (< 300 lines).
