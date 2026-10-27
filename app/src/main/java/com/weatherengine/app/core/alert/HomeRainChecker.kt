package com.weatherengine.app.core.alert

import com.weatherengine.app.data.openmeteo.OpenMeteoApiService

data class HomeRainResult(
    val isRaining: Boolean,
    val description: String,
    val temperatureCelsius: Double?,
    val precipitationMm: Double?
)

object HomeRainChecker {

    private val RAIN_WEATHER_CODES = setOf(
        51, 53, 55, // Drizzle (light, moderate, dense)
        56, 57,     // Freezing drizzle
        61, 63, 65, // Rain (slight, moderate, heavy)
        66, 67,     // Freezing rain
        80, 81, 82, // Rain showers
        95, 96, 99  // Thunderstorm with rain/hail
    )

    fun isRainWeather(code: Int?, precipitationMm: Double?): Boolean {
        val codeMatch = code != null && code in RAIN_WEATHER_CODES
        val precipMatch = precipitationMm != null && precipitationMm > 0.1
        return codeMatch || precipMatch
    }

    fun describeRain(code: Int?, precipMm: Double?): String {
        val precipStr = precipMm?.takeIf { it > 0.0 }?.let { " (${it} mm)" } ?: ""
        return when (code) {
            51 -> "Light drizzle$precipStr"
            53 -> "Moderate drizzle$precipStr"
            55 -> "Dense drizzle$precipStr"
            56, 57 -> "Freezing drizzle$precipStr"
            61 -> "Light rain$precipStr"
            63 -> "Moderate rain$precipStr"
            65 -> "Heavy torrential rain$precipStr"
            66, 67 -> "Freezing rain$precipStr"
            80 -> "Light rain showers$precipStr"
            81 -> "Moderate rain showers$precipStr"
            82 -> "Violent rain showers$precipStr"
            95 -> "Thunderstorm with rain$precipStr"
            96, 99 -> "Severe thunderstorm with rain & hail$precipStr"
            else -> if (precipMm != null && precipMm > 0.0) "Rain (${precipMm} mm)" else "Clear / No rain"
        }
    }

    suspend fun checkRainAt(
        apiService: OpenMeteoApiService,
        lat: Double,
        lon: Double
    ): HomeRainResult? {
        return try {
            val response = apiService.getForecast(latitude = lat, longitude = lon)
            if (!response.isSuccessful) return null
            val current = response.body()?.current ?: return null
            val code = current.weatherCode
            val precip = current.precipitation
            val raining = isRainWeather(code, precip)
            HomeRainResult(
                isRaining = raining,
                description = describeRain(code, precip),
                temperatureCelsius = current.temperature2m,
                precipitationMm = precip
            )
        } catch (_: Exception) {
            null
        }
    }
}
