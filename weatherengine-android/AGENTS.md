# WeatherEngine Android App

## Guidelines
- **Architecture**: MVVM with Jetpack Compose (Material 3) and package-by-feature layout.
- **Dependency Injection**: Manual wiring through `AppContainer` created in `WeatherEngineApp`.
- **Weather API Rule**: The app NEVER calls third-party weather APIs directly — all traffic routes strictly through our backend (or in-memory mock).
- **Security & Privacy**:
  - Never log tokens, passwords, authorization headers, or request/response bodies containing user credentials.
  - Cleartext HTTP traffic is only permitted in debug builds for local emulator testing via `network_security_config.xml`. Release builds strictly require HTTPS.
- **File Limits**: Keep files modular and concise (< 300 lines).
