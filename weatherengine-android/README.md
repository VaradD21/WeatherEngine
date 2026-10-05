# WeatherEngine Android App

Native Android client for **WeatherEngine**, built with **Kotlin**, **Jetpack Compose (Material 3)**, **Coroutines/Flow**, **Retrofit + Kotlinx Serialization**, and **DataStore Preferences**.

---

## Key Features
- **Real-Time & 7-Day Weather Forecast**: Current conditions, 24-hour horizontal strip, and 7-day daily range bars powered by Open-Meteo Forecast & Air Quality data.
- **3-Tier Resilient Data Architecture**:
  1. **Backend Proxy (`/api/weather`)**: Fetches Redis-cached weather + air quality bundle from the WeatherEngine backend.
  2. **Direct Open-Meteo Fallback**: Automatically falls back to querying Open-Meteo Forecast & Air Quality APIs directly if the backend is unreachable.
  3. **Offline DataStore Cache**: Serves the most recent cached forecast and homepage widgets when offline.
- **On-Device Persona Engine**:
  - **Health Conscious**: US AQI & PM2.5 breakdown, UV Index & protection guidance, Humidity comfort, and Pollen levels.
  - **Outdoor & Fitness**: Daylight & sunrise/sunset countdown, Best 2-hour Running Window scorer (0–100), Wind & gusts, and Heat index alerts.
  - **Daily Commuter**: Visibility rating, Thunderstorm/Fog lookahead alerts, 6-hour Commute Rain probability, and Traffic conditions.
- **Location Support**: One-tap GPS location via Google Play Services FusedLocationProvider + manual coordinate override.

---

## Project Structure

```text
app/src/main/java/com/weatherengine/app/
├── MainActivity.kt                        # WeatherEngineApp, Screen routes, MainActivity & NavHost
├── core/
│   ├── ErrorUtils.kt                      # Structured API error message parser
│   ├── Validators.kt                      # Email/password/coordinate validators, UrlUtils & Formatters
│   ├── WidgetMapper.kt                    # WidgetUi sealed hierarchy & backend JSON widget mapper
│   └── forecast/
│       ├── ForecastMapper.kt              # Maps Open-Meteo DTOs to ForecastUi & persona rules
│       └── ForecastUiModels.kt            # ForecastUi, CurrentWeatherUi, DayForecastUi & WmoWeatherCode
├── data/
│   ├── api/
│   │   └── ApiService.kt                  # Backend Retrofit API, AuthInterceptor & NetworkResult
│   ├── di/
│   │   └── AppContainer.kt                # Manual dependency injection container
│   ├── local/
│   │   └── SettingsStore.kt               # Jetpack DataStore preferences & offline cache
│   ├── location/
│   │   ├── AndroidLocationProvider.kt     # FusedLocationProvider + Geocoder reverse lookup
│   │   └── LocationProvider.kt            # LocationProvider interface & ResolvedLocation model
│   ├── model/
│   │   └── AuthModels.kt                  # Auth, Persona, and Homepage request/response DTOs
│   ├── openmeteo/
│   │   └── OpenMeteoClient.kt             # Direct Open-Meteo Retrofit service, client & DTOs
│   └── repository/
│       ├── OpenMeteoForecastRepository.kt # 3-tier forecast repository (Backend -> OpenMeteo -> Cache)
│       └── RemoteRepository.kt            # Auth, Persona, and Homepage backend repository
├── persona/
│   ├── PersonaModels.kt                   # Persona constants, domain models & widget types
│   ├── PersonaRules.kt                    # Pure domain rules (UV, AQI, daylight, heat, storm/fog, rain)
│   └── RunningRules.kt                    # 2-hour optimal running window scoring algorithm
└── ui/
    ├── auth/                              # AuthScreen & AuthViewModel (Login, Signup, Guest skip)
    ├── home/                              # HomeScreen, HomeViewModel & Persona/Forecast Compose cards
    ├── personas/                          # PersonaPickerScreen & PersonaPickerViewModel
    ├── settings/                          # SettingsScreen & SettingsViewModel
    └── theme/                             # Material 3 Dark/Light Theme, Color palette & Typography
```

---

## Running & Testing

### 1. Open in Android Studio
1. Open **Android Studio** $\rightarrow$ **File $\rightarrow$ Open** and select the `weatherengine-android` directory.
2. Wait for Gradle Sync to finish, select an Emulator or connected Android device (API 26+), and click **Run (`Shift + F10`)**.

### 2. Run via Command Line (PowerShell)
```powershell
# Run JVM Unit Tests
.\gradlew.bat testDebugUnitTest

# Build and install Debug APK onto a running emulator/device
.\gradlew.bat installDebug
```

### 3. Configuring Backend URL
- **Android Emulator**: Defaults to `http://10.0.2.2:8080` (maps to `localhost:8080` on your host machine).
- **Physical Device / Cloud**: Open the in-app **Settings** screen and set the Server URL to your LAN IP or deployed HTTPS backend URL.
