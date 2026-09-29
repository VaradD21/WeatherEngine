package com.weatherengine.app.persona

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object PersonaRules {

    fun uvCategory(uv: Double?): String {
        if (uv == null || uv < PersonaConstants.MIN_UV || uv > PersonaConstants.MAX_UV) return "Unavailable"
        return when {
            uv <= PersonaConstants.UV_LOW_MAX -> "Low"
            uv <= PersonaConstants.UV_MODERATE_MAX -> "Moderate"
            uv <= PersonaConstants.UV_HIGH_MAX -> "High"
            uv <= PersonaConstants.UV_VERY_HIGH_MAX -> "Very high"
            else -> "Extreme"
        }
    }

    fun humidityComfort(humidity: Int?): String {
        if (humidity == null || humidity < PersonaConstants.MIN_HUMIDITY || humidity > PersonaConstants.MAX_HUMIDITY) return "Unavailable"
        return when {
            humidity <= PersonaConstants.HUMIDITY_DRY_MAX -> "Dry"
            humidity <= PersonaConstants.HUMIDITY_COMFORTABLE_MAX -> "Comfortable"
            humidity <= PersonaConstants.HUMIDITY_HUMID_MAX -> "Humid"
            else -> "Very humid"
        }
    }

    fun usAqiCategory(aqi: Int?): String {
        if (aqi == null || aqi < 0) return "Unavailable"
        return when {
            aqi <= PersonaConstants.AQI_GOOD_MAX -> "Good"
            aqi <= PersonaConstants.AQI_MODERATE_MAX -> "Moderate"
            aqi <= PersonaConstants.AQI_SENSITIVE_MAX -> "Unhealthy for sensitive groups"
            aqi <= PersonaConstants.AQI_UNHEALTHY_MAX -> "Unhealthy"
            aqi <= PersonaConstants.AQI_VERY_UNHEALTHY_MAX -> "Very unhealthy"
            else -> "Hazardous"
        }
    }

    fun visibilityCategory(metres: Double?): String {
        if (metres == null || metres < PersonaConstants.MIN_VISIBILITY_M) return "Unavailable"
        return when {
            metres < PersonaConstants.VISIBILITY_VERY_POOR_MAX + 1 -> "Very poor"
            metres < PersonaConstants.VISIBILITY_POOR_MAX + 1 -> "Poor"
            metres < PersonaConstants.VISIBILITY_MODERATE_MAX + 1 -> "Moderate"
            else -> "Good"
        }
    }

    fun windCategory(kmh: Double?): String {
        if (kmh == null || kmh < 0.0) return "Unavailable"
        return when {
            kmh <= PersonaConstants.WIND_LIGHT_MAX -> "Light"
            kmh <= PersonaConstants.WIND_BREEZY_MAX -> "Breezy"
            kmh <= PersonaConstants.WIND_STRONG_MAX -> "Strong"
            else -> "Very strong"
        }
    }

    fun daylightInfo(
        currentLocalStr: String?,
        sunriseLocalStr: String?,
        sunsetLocalStr: String?
    ): DaylightState {
        if (currentLocalStr.isNullOrBlank() || sunriseLocalStr.isNullOrBlank() || sunsetLocalStr.isNullOrBlank()) {
            return DaylightState.Unavailable
        }
        return try {
            val current = LocalDateTime.parse(currentLocalStr)
            val sunrise = LocalDateTime.parse(sunriseLocalStr)
            val sunset = LocalDateTime.parse(sunsetLocalStr)

            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            val sunriseFormatted = sunrise.format(timeFormatter)
            val sunsetFormatted = sunset.format(timeFormatter)

            val dayLengthMin = Duration.between(sunrise, sunset).toMinutes().coerceAtLeast(0)
            val dayLengthFormatted = "${dayLengthMin / 60}h ${dayLengthMin % 60}m"

            when {
                current.isBefore(sunrise) -> {
                    val untilMin = Duration.between(current, sunrise).toMinutes().coerceAtLeast(0)
                    val formatted = "${untilMin / 60}h ${untilMin % 60}m"
                    DaylightState.BeforeSunrise(formatted, dayLengthFormatted, sunriseFormatted, sunsetFormatted)
                }
                current.isBefore(sunset) -> {
                    val untilMin = Duration.between(current, sunset).toMinutes().coerceAtLeast(0)
                    val formatted = "${untilMin / 60}h ${untilMin % 60}m"
                    DaylightState.Daylight(formatted, dayLengthFormatted, sunriseFormatted, sunsetFormatted)
                }
                else -> {
                    DaylightState.AfterSunset(dayLengthFormatted, sunriseFormatted, sunsetFormatted)
                }
            }
        } catch (_: Exception) {
            DaylightState.Unavailable
        }
    }

    fun hourlyWindow(
        allHours: List<HourlyRowUi>,
        currentTimeLocalStr: String?,
        count: Int = 24
    ): List<HourlyRowUi> {
        if (allHours.isEmpty()) return emptyList()
        val currentTruncated = currentTimeLocalStr?.substringBefore(":")?.let { "$it:00" }

        val startIndex = if (currentTruncated != null) {
            val exact = allHours.indexOfFirst { it.timeIso.startsWith(currentTruncated) }
            if (exact >= 0) exact else allHours.indexOfFirst { it.timeIso >= currentTruncated }.coerceAtLeast(0)
        } else {
            0
        }

        val window = allHours.drop(startIndex).take(count)
        return window.mapIndexed { index, row ->
            if (index == 0) {
                row.copy(timeLabel = "Now")
            } else {
                row
            }
        }
    }

    fun bestRunningWindow(hours: List<HourlyRowUi>): BestRunningResult {
        return RunningRules.bestRunningWindow(hours)
    }

    fun heatStatus(next24hApparentMax: Double?): HeatStatus {
        if (next24hApparentMax == null) return HeatStatus.None
        return when {
            next24hApparentMax >= PersonaConstants.HEAT_ALERT_MIN -> {
                HeatStatus.Alert(
                    active = true,
                    message = "Heat Alert: Feels like ${next24hApparentMax.roundToInt()}°C",
                    note = PersonaConstants.HEAT_DISCLAIMER_NOTE
                )
            }
            next24hApparentMax >= PersonaConstants.HEAT_CAUTION_MIN -> {
                HeatStatus.Caution(
                    message = "Heat Caution: Feels like ${next24hApparentMax.roundToInt()}°C",
                    note = PersonaConstants.HEAT_DISCLAIMER_NOTE
                )
            }
            else -> HeatStatus.None
        }
    }

    fun stormFogStatus(hours: List<HourlyRowUi>, @Suppress("UNUSED_PARAMETER") nowIso: String? = null): StormFogStatus {
        val next12 = hours.take(PersonaConstants.STORM_LOOKAHEAD_HOURS)
        val stormHour = next12.firstOrNull { it.weatherCode in listOf(95, 96, 99) }

        if (stormHour != null) {
            val formatted = formatHourCompact(stormHour.timeIso)
            return StormFogStatus(
                alertActive = true,
                stormHour = formatted,
                message = "Thunderstorm expected around $formatted"
            )
        }

        val next6 = hours.take(PersonaConstants.FOG_LOOKAHEAD_HOURS)
        val fogHour = next6.firstOrNull {
            it.weatherCode in listOf(45, 48) || (it.visibilityM != null && it.visibilityM < 1000.0)
        }

        if (fogHour != null) {
            val formatted = formatHourCompact(fogHour.timeIso)
            return StormFogStatus(
                alertActive = true,
                fogHour = formatted,
                message = "Low visibility / Fog around $formatted"
            )
        }

        return StormFogStatus(alertActive = false, message = "No severe weather or fog expected")
    }

    fun commuteRain(hours: List<HourlyRowUi>): CommuteRainResult {
        val next6 = hours.take(PersonaConstants.COMMUTE_LOOKAHEAD_HOURS)
        if (next6.isEmpty()) {
            return CommuteRainResult(0, "—", "Dry commute expected", null)
        }

        var peakChance = 0
        var peakHourIso = next6.first().timeIso
        var minVisibility: Double? = null

        for (hour in next6) {
            val prob = hour.precipProb ?: 0
            if (prob >= peakChance) {
                peakChance = prob
                peakHourIso = hour.timeIso
            }
            hour.visibilityM?.let { vis ->
                minVisibility = if (minVisibility == null) vis else minOf(minVisibility!!, vis)
            }
        }

        val advice = when {
            peakChance >= PersonaConstants.COMMUTE_RAIN_HIGH_THRESHOLD -> "Take an umbrella"
            peakChance >= PersonaConstants.COMMUTE_RAIN_MODERATE_THRESHOLD -> "Slight chance of rain"
            else -> "Dry commute expected"
        }

        val peakLabel = formatHourCompact(peakHourIso)
        return CommuteRainResult(peakChance, peakLabel, advice, minVisibility)
    }

    fun mergedWidgets(selectedCodes: Set<String>): List<PersonaWidgetType> {
        val lookup = mapOf(
            "health_conscious" to listOf(
                PersonaWidgetType.AQI,
                PersonaWidgetType.UV,
                PersonaWidgetType.HUMIDITY,
                PersonaWidgetType.POLLEN
            ),
            "outdoor_fitness" to listOf(
                PersonaWidgetType.SUN,
                PersonaWidgetType.BEST_RUNNING,
                PersonaWidgetType.WIND,
                PersonaWidgetType.HEAT
            ),
            "commuter" to listOf(
                PersonaWidgetType.VISIBILITY,
                PersonaWidgetType.STORM_FOG,
                PersonaWidgetType.COMMUTE_RAIN,
                PersonaWidgetType.TRAFFIC
            )
        )

        val personaOrder = listOf("health_conscious", "outdoor_fitness", "commuter")
        val result = mutableListOf<PersonaWidgetType>()

        for (persona in personaOrder) {
            if (selectedCodes.contains(persona)) {
                lookup[persona]?.let { widgets ->
                    for (w in widgets) {
                        if (!result.contains(w)) {
                            result.add(w)
                        }
                    }
                }
            }
        }
        return result
    }

    private fun formatHourCompact(iso: String): String {
        return try {
            val time = LocalDateTime.parse(iso)
            time.format(DateTimeFormatter.ofPattern("h a", Locale.US))
        } catch (_: Exception) {
            iso
        }
    }

    private fun plusOneHour(iso: String): String {
        return try {
            val time = LocalDateTime.parse(iso)
            time.plusHours(1).toString()
        } catch (_: Exception) {
            iso
        }
    }
}
