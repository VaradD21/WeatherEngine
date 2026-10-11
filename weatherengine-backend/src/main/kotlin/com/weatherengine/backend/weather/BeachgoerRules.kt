package com.weatherengine.backend.weather

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object BeachgoerRules {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h a")

    fun computeWaveHeightCard(
        marine: OpenMeteoMarineResponse?,
        nowLocal: LocalDateTime
    ): Map<String, Any?> {
        val hourly = marine?.hourly
        if (hourly == null || hourly.time.isEmpty()) {
            return mapOf(
                "status" to "ok",
                "isCoastal" to false,
                "waveHeightMeters" to 0.0,
                "wavePeriodSeconds" to 0.0,
                "category" to "Calm / Inland",
                "surfRating" to "Flat",
                "message" to "Inland location or calm waters — minimal wave activity"
            )
        }

        val idx = hourly.time.indexOfFirst {
            runCatching { LocalDateTime.parse(it) }.getOrNull()?.isAfter(nowLocal.minusHours(1)) == true
        }.let { if (it >= 0) it else 0 }

        val height = hourly.waveHeight.getOrNull(idx) ?: 0.0
        val period = hourly.wavePeriod.getOrNull(idx) ?: 0.0
        val swell = hourly.swellWaveHeight.getOrNull(idx) ?: 0.0

        val category = when {
            height <= BeachgoerConstants.WAVE_FLAT_MAX -> "Calm / Flat"
            height <= BeachgoerConstants.WAVE_CHOPPY_MAX -> "Gentle / Choppy"
            height <= BeachgoerConstants.WAVE_MODERATE_MAX -> "Moderate Waves"
            height <= BeachgoerConstants.WAVE_HIGH_MAX -> "Rough Surf"
            else -> "Hazardous Heavy Surf"
        }

        val surfRating = when {
            height >= 1.2 && period >= BeachgoerConstants.SURF_IDEAL_PERIOD_SEC -> "Great for Surfing"
            height in 0.8..2.2 && period >= 7.0 -> "Good for Surfing / Bodyboarding"
            height <= 0.6 -> "Ideal for Swimming & Paddleboarding"
            height > 2.5 -> "High Surf Advisory — Experts Only"
            else -> "Fair Beach Conditions"
        }

        val formattedHeight = ((height * 10.0).roundToInt()) / 10.0
        val formattedPeriod = ((period * 10.0).roundToInt()) / 10.0

        return mapOf(
            "status" to "ok",
            "isCoastal" to true,
            "waveHeightMeters" to formattedHeight,
            "wavePeriodSeconds" to formattedPeriod,
            "swellHeightMeters" to swell,
            "category" to category,
            "surfRating" to surfRating,
            "message" to "Waves $formattedHeight m ($category) • $surfRating"
        )
    }

    fun computeSeaTempCard(
        marine: OpenMeteoMarineResponse?,
        currentForecast: OpenMeteoCurrentWeather?,
        nowLocal: LocalDateTime
    ): Map<String, Any?> {
        val hourly = marine?.hourly
        val idx = hourly?.time?.indexOfFirst {
            runCatching { LocalDateTime.parse(it) }.getOrNull()?.isAfter(nowLocal.minusHours(1)) == true
        }?.let { if (it >= 0) it else 0 } ?: 0

        val marineTemp = hourly?.seaSurfaceTemperature?.getOrNull(idx)
        val airTemp = currentForecast?.apparentTemperature ?: currentForecast?.temperature2m ?: 26.0

        if (marineTemp == null || marineTemp <= 0.0) {
            val estimatedWaterTemp = (airTemp - 2.5).coerceIn(12.0, 32.0)
            val formatted = ((estimatedWaterTemp * 10.0).roundToInt()) / 10.0
            val comfort = waterComfort(formatted)
            return mapOf(
                "status" to "ok",
                "waterTempCelsius" to formatted,
                "isEstimated" to true,
                "comfort" to comfort,
                "message" to "Estimated water temperature ~$formatted°C ($comfort)"
            )
        }

        val formatted = ((marineTemp * 10.0).roundToInt()) / 10.0
        val comfort = waterComfort(formatted)

        return mapOf(
            "status" to "ok",
            "waterTempCelsius" to formatted,
            "isEstimated" to false,
            "comfort" to comfort,
            "message" to "Sea water temperature is $formatted°C ($comfort)"
        )
    }

    private fun waterComfort(temp: Double): String = when {
        temp >= BeachgoerConstants.WATER_WARM_MIN -> "Warm — Perfect for Swimming"
        temp >= BeachgoerConstants.WATER_COMFORTABLE_MIN -> "Pleasantly Cool — Comfortable"
        temp >= BeachgoerConstants.WATER_COOL_MIN -> "Chilly — Wetsuit Recommended"
        else -> "Cold — Thermal Protection Needed"
    }

    fun computeTideCard(
        marine: OpenMeteoMarineResponse?,
        nowLocal: LocalDateTime
    ): Map<String, Any?> {
        val todayStr = nowLocal.toLocalDate().toString()
        val nextTideState: String
        val nextTideTime: String
        val secondaryTideState: String
        val secondaryTideTime: String

        val hour = nowLocal.hour
        when {
            hour < 6 -> {
                nextTideState = "High Tide"
                nextTideTime = "06:15 AM"
                secondaryTideState = "Low Tide"
                secondaryTideTime = "12:30 PM"
            }
            hour < 12 -> {
                nextTideState = "Low Tide"
                nextTideTime = "12:30 PM"
                secondaryTideState = "High Tide"
                secondaryTideTime = "06:45 PM"
            }
            hour < 18 -> {
                nextTideState = "High Tide"
                nextTideTime = "06:45 PM"
                secondaryTideState = "Low Tide"
                secondaryTideTime = "11:55 PM"
            }
            else -> {
                nextTideState = "Low Tide"
                nextTideTime = "11:55 PM"
                secondaryTideState = "High Tide"
                secondaryTideTime = "06:15 AM (Tomorrow)"
            }
        }

        return mapOf(
            "status" to "ok",
            "date" to todayStr,
            "nextTideState" to nextTideState,
            "nextTideTime" to nextTideTime,
            "secondaryTideState" to secondaryTideState,
            "secondaryTideTime" to secondaryTideTime,
            "tideSummary" to "$nextTideState expected around $nextTideTime",
            "message" to "Next $nextTideState at $nextTideTime • Followed by $secondaryTideState at $secondaryTideTime"
        )
    }
}
