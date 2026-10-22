package com.weatherengine.backend.alert

import com.weatherengine.backend.location.PredictiveWeatherAlertDto
import com.weatherengine.backend.user.UserHealthProfile
import com.weatherengine.backend.weather.WeatherBundleDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Locale

@Service
class WeatherAlertService {

    fun evaluateAlerts(
        bundle: WeatherBundleDto,
        healthProfile: UserHealthProfile? = null,
        targetHour: Int = LocalDateTime.now().hour,
        lat: Double = bundle.forecast.latitude ?: 0.0,
        lon: Double = bundle.forecast.longitude ?: 0.0
    ): List<PredictiveWeatherAlertDto> {
        val alerts = mutableListOf<PredictiveWeatherAlertDto>()
        val timeFormatted = String.format(Locale.US, "%02d:00", targetHour)

        val cur = bundle.forecast.current
        val hourly = bundle.forecast.hourly
        val air = bundle.airQuality?.current
        val marine = bundle.marine?.hourly

        // 1. AQI Alert (User Health or Extreme Smog)
        val aqi = air?.usAqi
        val aqiThreshold = healthProfile?.aqiThreshold ?: 100
        if (aqi != null) {
            if (aqi >= 300) {
                alerts.add(
                    PredictiveWeatherAlertDto(
                        targetHour = targetHour,
                        targetTimeFormatted = timeFormatted,
                        predictedLatitude = lat,
                        predictedLongitude = lon,
                        alertType = "AQI_HAZARDOUS",
                        headline = "Hazardous Air Quality Warning (AQI $aqi)",
                        message = "Emergency conditions: AQI reached $aqi. Serious risk of respiratory impairment. Keep all windows shut and stay indoors.",
                        severity = "CRITICAL"
                    )
                )
            } else if (aqi >= aqiThreshold) {
                val category = when {
                    aqi > 200 -> "Very Unhealthy"
                    aqi > 150 -> "Unhealthy"
                    else -> "Unhealthy for Sensitive Groups"
                }
                alerts.add(
                    PredictiveWeatherAlertDto(
                        targetHour = targetHour,
                        targetTimeFormatted = timeFormatted,
                        predictedLatitude = lat,
                        predictedLongitude = lon,
                        alertType = "AQI_ALERT",
                        headline = "Air Quality Advisory (AQI $aqi)",
                        message = "AQI is $aqi ($category), exceeding your personalized threshold of $aqiThreshold. Sensitive individuals should avoid outdoor exercise.",
                        severity = if (aqi >= 200) "CRITICAL" else "WARNING"
                    )
                )
            }
        }

        // 2. Severe Weather (Thunderstorms, Storm, Heavy Snow, Dense Fog)
        val wmo = cur?.weatherCode ?: 0
        if (wmo in setOf(95, 96, 99)) {
            alerts.add(
                PredictiveWeatherAlertDto(
                    targetHour = targetHour,
                    targetTimeFormatted = timeFormatted,
                    predictedLatitude = lat,
                    predictedLongitude = lon,
                    alertType = "SEVERE_THUNDERSTORM",
                    headline = "Severe Thunderstorm & Lightning Warning",
                    message = "Active severe thunderstorm (WMO $wmo) detected. High risk of lightning, heavy downpours, and violent squalls. Seek immediate shelter.",
                    severity = "CRITICAL"
                )
            )
        } else if (wmo in setOf(45, 48)) {
            alerts.add(
                PredictiveWeatherAlertDto(
                    targetHour = targetHour,
                    targetTimeFormatted = timeFormatted,
                    predictedLatitude = lat,
                    predictedLongitude = lon,
                    alertType = "DENSE_FOG",
                    headline = "Dense Fog & Low Visibility Advisory",
                    message = "Dense fog (WMO $wmo) is severely reducing road visibility. Reduce speed and use low-beam headlights.",
                    severity = "WARNING"
                )
            )
        } else if (wmo in 65..67) {
            alerts.add(
                PredictiveWeatherAlertDto(
                    targetHour = targetHour,
                    targetTimeFormatted = timeFormatted,
                    predictedLatitude = lat,
                    predictedLongitude = lon,
                    alertType = "HEAVY_RAIN",
                    headline = "Torrential Downpour & Flash Flood Warning",
                    message = "Intense rainfall detected. Waterlogging and severe traffic delays expected.",
                    severity = "WARNING"
                )
            )
        }

        // 3. Heat Wave / Extreme Heat Index
        val feelsLike = cur?.apparentTemperature ?: cur?.temperature2m
        val heatThreshold = if (healthProfile?.hasHeatSensitivity == true) 35.0 else 40.0
        if (feelsLike != null && feelsLike >= heatThreshold) {
            alerts.add(
                PredictiveWeatherAlertDto(
                    targetHour = targetHour,
                    targetTimeFormatted = timeFormatted,
                    predictedLatitude = lat,
                    predictedLongitude = lon,
                    alertType = "HEAT_WAVE",
                    headline = "Dangerous Heat Wave Advisory (${feelsLike.toInt()}°C feels like)",
                    message = "Extreme heat index of ${feelsLike.toInt()}°C. High risk of heat stroke and exhaustion. Hydrate frequently and avoid sun exposure.",
                    severity = if (feelsLike >= 44.0) "CRITICAL" else "WARNING"
                )
            )
        }

        // 4. Extreme UV Index
        val uv = hourly?.uvIndex?.firstOrNull { it != null } ?: bundle.forecast.daily?.uvIndexMax?.firstOrNull()
        val uvThreshold = healthProfile?.uvThreshold?.toDouble() ?: 8.0
        if (uv != null && uv >= uvThreshold) {
            alerts.add(
                PredictiveWeatherAlertDto(
                    targetHour = targetHour,
                    targetTimeFormatted = timeFormatted,
                    predictedLatitude = lat,
                    predictedLongitude = lon,
                    alertType = "UV_ALERT",
                    headline = "Extreme UV Radiation Alert (Index ${uv.toInt()})",
                    message = "Solar UV radiation reached ${uv.toInt()}, exceeding safe limits. Apply SPF 50+, wear UV-blocking sunglasses, and stay in shade.",
                    severity = if (uv >= 11.0) "CRITICAL" else "WARNING"
                )
            )
        }

        // 5. High Wind / Gale Warning
        val wind = cur?.windSpeed10m ?: 0.0
        val gusts = cur?.windGusts10m ?: wind
        if (wind >= 55.0 || gusts >= 70.0) {
            alerts.add(
                PredictiveWeatherAlertDto(
                    targetHour = targetHour,
                    targetTimeFormatted = timeFormatted,
                    predictedLatitude = lat,
                    predictedLongitude = lon,
                    alertType = "WIND_GALE",
                    headline = "Gale Force Wind Advisory (${wind.toInt()} km/h)",
                    message = "Damaging wind gusts reaching ${gusts.toInt()} km/h. Secure outdoor objects and be cautious of falling tree branches.",
                    severity = "WARNING"
                )
            )
        }

        // 6. Marine High Waves
        val waveHeight = marine?.waveHeight?.firstOrNull { it != null }
        if (waveHeight != null && waveHeight >= 3.0) {
            alerts.add(
                PredictiveWeatherAlertDto(
                    targetHour = targetHour,
                    targetTimeFormatted = timeFormatted,
                    predictedLatitude = lat,
                    predictedLongitude = lon,
                    alertType = "MARINE_ALERT",
                    headline = "Rough Seas & High Surf Warning (${String.format(Locale.US, "%.1f", waveHeight)}m)",
                    message = "Dangerous breaking waves of ${String.format(Locale.US, "%.1f", waveHeight)}m. Boating, swimming, and surfing are strictly hazardous.",
                    severity = if (waveHeight >= 5.0) "CRITICAL" else "WARNING"
                )
            )
        }

        // 7. Pollen Bloom
        val maxPollen = listOfNotNull(air?.grassPollen, air?.birchPollen, air?.alderPollen, air?.ragweedPollen).maxOrNull() ?: 0.0
        if (maxPollen >= 80.0 && healthProfile?.hasAllergies == true) {
            alerts.add(
                PredictiveWeatherAlertDto(
                    targetHour = targetHour,
                    targetTimeFormatted = timeFormatted,
                    predictedLatitude = lat,
                    predictedLongitude = lon,
                    alertType = "POLLEN_ALERT",
                    headline = "Very High Pollen Bloom Alert",
                    message = "Pollen counts have surged (${maxPollen.toInt()} grains/m³). Allergy symptoms may flare up; keep anti-histamines on hand.",
                    severity = "INFO"
                )
            )
        }

        return alerts
    }
}
