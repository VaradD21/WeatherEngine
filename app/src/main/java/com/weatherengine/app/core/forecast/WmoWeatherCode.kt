package com.weatherengine.app.core.forecast

data class WmoWeatherInfo(
    val code: Int,
    val description: String,
    val iconKey: String
)

object WmoWeatherCode {

    fun info(code: Int?, isDay: Boolean = true): WmoWeatherInfo {
        if (code == null) {
            return WmoWeatherInfo(-1, "Unknown", "unknown")
        }
        return when (code) {
            0 -> WmoWeatherInfo(
                code = 0,
                description = if (isDay) "Sunny" else "Clear",
                iconKey = if (isDay) "clear_day" else "clear_night"
            )
            1 -> WmoWeatherInfo(
                code = 1,
                description = "Mainly clear",
                iconKey = if (isDay) "mainly_clear_day" else "mainly_clear_night"
            )
            2 -> WmoWeatherInfo(
                code = 2,
                description = "Partly cloudy",
                iconKey = if (isDay) "partly_cloudy_day" else "partly_cloudy_night"
            )
            3 -> WmoWeatherInfo(
                code = 3,
                description = "Overcast",
                iconKey = "cloudy"
            )
            45, 48 -> WmoWeatherInfo(
                code = code,
                description = "Foggy",
                iconKey = "fog"
            )
            51, 53, 55 -> WmoWeatherInfo(
                code = code,
                description = "Drizzle",
                iconKey = "drizzle"
            )
            56, 57 -> WmoWeatherInfo(
                code = code,
                description = "Freezing drizzle",
                iconKey = "drizzle"
            )
            61 -> WmoWeatherInfo(
                code = 61,
                description = "Slight rain",
                iconKey = "rain"
            )
            63 -> WmoWeatherInfo(
                code = 63,
                description = "Moderate rain",
                iconKey = "rain"
            )
            65 -> WmoWeatherInfo(
                code = 65,
                description = "Heavy rain",
                iconKey = "heavy_rain"
            )
            66, 67 -> WmoWeatherInfo(
                code = code,
                description = "Freezing rain",
                iconKey = "rain"
            )
            71, 73, 75, 77 -> WmoWeatherInfo(
                code = code,
                description = "Snow",
                iconKey = "snow"
            )
            80, 81 -> WmoWeatherInfo(
                code = code,
                description = "Rain showers",
                iconKey = "rain_showers"
            )
            82 -> WmoWeatherInfo(
                code = 82,
                description = "Violent rain showers",
                iconKey = "heavy_rain"
            )
            85, 86 -> WmoWeatherInfo(
                code = code,
                description = "Snow showers",
                iconKey = "snow"
            )
            95 -> WmoWeatherInfo(
                code = 95,
                description = "Thunderstorm",
                iconKey = "thunderstorm"
            )
            96, 99 -> WmoWeatherInfo(
                code = code,
                description = "Thunderstorm with hail",
                iconKey = "thunderstorm"
            )
            else -> WmoWeatherInfo(
                code = code,
                description = "Weather ($code)",
                iconKey = "unknown"
            )
        }
    }
}
