package com.weatherengine.app.core.forecast

import com.weatherengine.app.persona.BestRunningResult
import com.weatherengine.app.persona.CommuteRainResult
import com.weatherengine.app.persona.DaylightState
import com.weatherengine.app.persona.HeatStatus
import com.weatherengine.app.persona.HourlyRowUi
import com.weatherengine.app.persona.PollenInfo
import com.weatherengine.app.persona.StormFogStatus
import kotlinx.serialization.Serializable

@Serializable
data class ForecastUi(
    val locationLabel: String,
    val updatedAtLabel: String,
    val timestampMs: Long,
    val current: CurrentWeatherUi,
    val hourly: List<HourlyRowUi> = emptyList(),
    val days: List<DayForecastUi> = emptyList(),
    val daylight: DaylightState = DaylightState.Unavailable,
    val bestRunning: BestRunningResult = BestRunningResult.NoGoodWindow("Unavailable"),
    val heat: HeatStatus = HeatStatus.None,
    val stormFog: StormFogStatus = StormFogStatus(false, message = "Clear conditions"),
    val commute: CommuteRainResult = CommuteRainResult(0, "—", "Dry commute expected"),
    val pollen: PollenInfo = PollenInfo(),
    val uvMaxToday: Double? = null,
    val attribution: String = "Weather data by Open-Meteo.com",
    val isCached: Boolean = false
)

@Serializable
data class CurrentWeatherUi(
    val temperatureFormatted: String,
    val apparentTemperatureFormatted: String,
    val condition: String,
    val iconKey: String,
    val humidityFormatted: String,
    val humidityPercent: Int? = null,
    val windFormatted: String,
    val windSpeedKmh: Double? = null,
    val windGustsKmh: Double? = null,
    val precipitationFormatted: String,
    val visibilityMeters: Double? = null,
    val uvIndex: Double? = null,
    val aqi: Int? = null,
    val aqiCategory: String = "Unavailable",
    val pm25: Double? = null
)

@Serializable
data class DayForecastUi(
    val dateIso: String,
    val dayLabel: String,
    val condition: String,
    val iconKey: String,
    val maxTempFormatted: String,
    val minTempFormatted: String,
    val precipitationProbability: Int? = null,
    val rangeFractionStart: Float = 0f,
    val rangeFractionEnd: Float = 1f
)

data class WmoWeatherInfo(
    val description: String,
    val iconKey: String
)

object WmoWeatherCode {

    fun info(code: Int?, isDay: Boolean = true): WmoWeatherInfo {
        if (code == null) {
            return WmoWeatherInfo("Unknown", "unknown")
        }
        return when (code) {
            0 -> WmoWeatherInfo(
                description = if (isDay) "Sunny" else "Clear",
                iconKey = if (isDay) "clear_day" else "clear_night"
            )
            1 -> WmoWeatherInfo(
                description = "Mainly clear",
                iconKey = if (isDay) "mainly_clear_day" else "mainly_clear_night"
            )
            2 -> WmoWeatherInfo(
                description = "Partly cloudy",
                iconKey = if (isDay) "partly_cloudy_day" else "partly_cloudy_night"
            )
            3 -> WmoWeatherInfo(
                description = "Overcast",
                iconKey = "cloudy"
            )
            45, 48 -> WmoWeatherInfo(
                description = "Foggy",
                iconKey = "fog"
            )
            51, 53, 55 -> WmoWeatherInfo(
                description = "Drizzle",
                iconKey = "drizzle"
            )
            56, 57 -> WmoWeatherInfo(
                description = "Freezing drizzle",
                iconKey = "drizzle"
            )
            61 -> WmoWeatherInfo(
                description = "Slight rain",
                iconKey = "rain"
            )
            63 -> WmoWeatherInfo(
                description = "Moderate rain",
                iconKey = "rain"
            )
            65 -> WmoWeatherInfo(
                description = "Heavy rain",
                iconKey = "heavy_rain"
            )
            66, 67 -> WmoWeatherInfo(
                description = "Freezing rain",
                iconKey = "rain"
            )
            71, 73, 75, 77 -> WmoWeatherInfo(
                description = "Snow",
                iconKey = "snow"
            )
            80, 81 -> WmoWeatherInfo(
                description = "Rain showers",
                iconKey = "rain_showers"
            )
            82 -> WmoWeatherInfo(
                description = "Violent rain showers",
                iconKey = "heavy_rain"
            )
            85, 86 -> WmoWeatherInfo(
                description = "Snow showers",
                iconKey = "snow"
            )
            95 -> WmoWeatherInfo(
                description = "Thunderstorm",
                iconKey = "thunderstorm"
            )
            96, 99 -> WmoWeatherInfo(
                description = "Thunderstorm with hail",
                iconKey = "thunderstorm"
            )
            else -> WmoWeatherInfo(
                description = "Weather ($code)",
                iconKey = "unknown"
            )
        }
    }
}

