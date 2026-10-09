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
- **Adding a persona**:
  1. Registry entry: Define persona code and widget codes in backend `data.sql` and `WidgetBuilder` registry.
  2. Mapper case: Map widget payloads into sealed `WidgetUi` models in Android `WidgetMapper.kt`.
  3. Constants: Store thresholds in backend `ParentPersonaConstants.kt` and UI metadata in Android `PersonaConstants.kt`.
