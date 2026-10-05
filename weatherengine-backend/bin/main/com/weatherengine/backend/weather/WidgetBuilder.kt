package com.weatherengine.backend.weather

import org.springframework.stereotype.Component
import kotlin.math.roundToInt

@Component
class WidgetBuilder {

    fun buildWidget(widgetCode: String, bundle: WeatherBundleDto): WidgetDto {
        val current = bundle.forecast.current
        val hourly = bundle.forecast.hourly
        val daily = bundle.forecast.daily
        val aqiCurrent = bundle.airQuality?.current

        val data: Map<String, Any?> = when (widgetCode) {
            "aqi_card" -> {
                val usAqi = aqiCurrent?.usAqi
                if (usAqi != null && usAqi >= 0) {
                    mapOf(
                        "aqi" to usAqi,
                        "category" to aqiCategory(usAqi),
                        "pm25" to aqiCurrent.pm25
                    )
                } else {
                    mapOf(
                        "status" to "unavailable",
                        "message" to "Air quality station data unavailable for this area"
                    )
                }
            }

            "humidity_card" -> {
                val humidity = current?.relativeHumidity2m ?: 50
                mapOf("humidityPercent" to humidity.coerceIn(0, 100))
            }

            "uv_index_card" -> {
                val uvNow = hourly?.uvIndex?.firstOrNull { it != null }
                    ?: daily?.uvIndexMax?.firstOrNull()
                if (uvNow != null) {
                    mapOf(
                        "uvIndex" to uvNow,
                        "message" to uvAdvice(uvNow)
                    )
                } else {
                    mapOf(
                        "status" to "unavailable",
                        "message" to "UV sensor index currently offline"
                    )
                }
            }

            "sunrise_sunset_card" -> {
                val sunrise = daily?.sunrise?.firstOrNull()
                val sunset = daily?.sunset?.firstOrNull()
                if (!sunrise.isNullOrBlank() && !sunset.isNullOrBlank()) {
                    mapOf("sunrise" to sunrise, "sunset" to sunset)
                } else {
                    mapOf(
                        "status" to "unavailable",
                        "message" to "Sunrise and sunset times unavailable"
                    )
                }
            }

            "wind_speed_card" -> {
                val windKmh = current?.windSpeed10m ?: 0.0
                val windMs = ((windKmh / 3.6) * 10.0).roundToInt() / 10.0
                mapOf(
                    "speedMetersPerSecond" to windMs,
                    "speedKmh" to windKmh
                )
            }

            "heat_alert_card" -> {
                val temp = current?.apparentTemperature ?: current?.temperature2m ?: 25.0
                val maxApparent = hourly?.apparentTemperature?.filterNotNull()?.take(24)?.maxOrNull() ?: temp
                val alertActive = maxApparent >= 32.0
                val msg = when {
                    maxApparent >= 38.0 -> "Extreme heat warning (${maxApparent.roundToInt()}°C feels-like). Stay hydrated and avoid midday sun."
                    maxApparent >= 32.0 -> "Heat caution (${maxApparent.roundToInt()}°C feels-like). Limit strenuous midday activity."
                    else -> "No active heat alerts for your area"
                }
                mapOf(
                    "alertActive" to alertActive,
                    "temperatureCelsius" to temp,
                    "message" to msg
                )
            }

            "traffic_card" -> {
                val precipProb = hourly?.precipitationProbability?.filterNotNull()?.take(6)?.maxOrNull() ?: 0
                val msg = when {
                    precipProb >= 60 -> "Rain expected (${precipProb}% chance) — allow extra commute time"
                    precipProb >= 30 -> "Light showers possible (${precipProb}% chance) during upcoming commute"
                    else -> "Dry road conditions expected for your commute"
                }
                mapOf(
                    "status" to "active",
                    "message" to msg
                )
            }

            "visibility_card" -> {
                val visMeters = hourly?.visibility?.firstOrNull { it != null }?.roundToInt() ?: 10000
                val category = when {
                    visMeters >= 9000 -> "Clear"
                    visMeters >= 4000 -> "Moderate"
                    visMeters >= 1000 -> "Low"
                    else -> "Dense Fog"
                }
                mapOf(
                    "visibilityMeters" to visMeters,
                    "category" to category
                )
            }

            "storm_fog_alert_card" -> {
                val code = current?.weatherCode ?: 0
                val stormOrFog = code in setOf(45, 48, 95, 96, 99) || code in 65..67 || code in 82..86
                val msg = if (stormOrFog) {
                    "Active weather advisory (WMO code $code) — exercise caution outdoors"
                } else {
                    "No storm or fog warnings in effect"
                }
                mapOf(
                    "alertActive" to stormOrFog,
                    "conditionCode" to code,
                    "message" to msg
                )
            }

            "pollen_card" -> {
                val pollens = listOfNotNull(
                    aqiCurrent?.grassPollen?.let { "Grass" to it },
                    aqiCurrent?.birchPollen?.let { "Birch" to it },
                    aqiCurrent?.alderPollen?.let { "Alder" to it },
                    aqiCurrent?.ragweedPollen?.let { "Ragweed" to it }
                )
                val top = pollens.maxByOrNull { it.second }
                if (top != null) {
                    mapOf(
                        "status" to "unavailable",
                        "message" to "${top.first} pollen: ${top.second.roundToInt()} grains/m³"
                    )
                } else {
                    mapOf(
                        "status" to "unavailable",
                        "message" to "Pollen count not reported for this region"
                    )
                }
            }

            "best_running_hours_card" -> {
                val temp = current?.temperature2m?.roundToInt() ?: 24
                mapOf(
                    "status" to "unavailable",
                    "message" to "Optimal outdoor workout conditions (~${temp}°C)"
                )
            }

            else -> {
                mapOf(
                    "status" to "unavailable",
                    "message" to "Widget $widgetCode is ready"
                )
            }
        }

        return WidgetDto(type = widgetCode, data = data)
    }

    private fun aqiCategory(usAqi: Int): String = when {
        usAqi <= 50 -> "Good"
        usAqi <= 100 -> "Moderate"
        usAqi <= 150 -> "Unhealthy for Sensitive Groups"
        usAqi <= 200 -> "Unhealthy"
        usAqi <= 300 -> "Very Unhealthy"
        else -> "Hazardous"
    }

    private fun uvAdvice(uv: Double): String = when {
        uv < 3.0 -> "Low UV — minimal protection needed"
        uv < 6.0 -> "Moderate UV — wear sunglasses and SPF 30+"
        uv < 8.0 -> "High UV — seek shade during midday hours"
        uv < 11.0 -> "Very High UV — extra sun protection required"
        else -> "Extreme UV — avoid direct midday sun"
    }
}
