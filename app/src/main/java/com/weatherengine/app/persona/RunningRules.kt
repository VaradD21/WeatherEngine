package com.weatherengine.app.persona

import com.weatherengine.app.core.Formatters
import java.time.LocalDateTime
import kotlin.math.roundToInt

object RunningRules {

    fun bestRunningWindow(hours: List<HourlyRowUi>): BestRunningResult {
        val window = hours.take(24)
        val candidates = window.mapNotNull { row ->
            try {
                val hourOfDay = LocalDateTime.parse(row.timeIso).hour
                if (hourOfDay in PersonaConstants.RUN_CANDIDATE_START_HOUR..PersonaConstants.RUN_CANDIDATE_END_HOUR) {
                    Pair(row, scoreRunningHour(row))
                } else null
            } catch (_: Exception) {
                null
            }
        }

        if (candidates.size < PersonaConstants.RUN_WINDOW_CONSECUTIVE_HOURS) {
            return BestRunningResult.NoGoodWindow("Not enough daylight hours remaining")
        }

        var bestAvgScore = -1.0
        var bestIndex = 0

        for (i in 0..candidates.size - PersonaConstants.RUN_WINDOW_CONSECUTIVE_HOURS) {
            val avg = (candidates[i].second + candidates[i + 1].second) / 2.0
            if (avg > bestAvgScore) {
                bestAvgScore = avg
                bestIndex = i
            }
        }

        val finalScore = bestAvgScore.roundToInt()
        if (finalScore < PersonaConstants.RUN_MIN_ACCEPTABLE_SCORE) {
            val mainReason = deduceRunningFailureReason(candidates.map { it.first })
            return BestRunningResult.NoGoodWindow(mainReason)
        }

        val firstRow = candidates[bestIndex].first
        val secondRow = candidates[bestIndex + 1].first
        val startLabel = Formatters.formatHourCompact(firstRow.timeIso)
        val endLabel = Formatters.formatHourCompact(plusOneHour(secondRow.timeIso))
        val reasons = extractRunningReasons(firstRow, secondRow)

        return BestRunningResult.Optimal(startLabel, endLabel, finalScore, reasons)
    }

    private fun scoreRunningHour(row: HourlyRowUi): Int {
        if (row.weatherCode in listOf(95, 96, 99)) return 0

        var score = 100
        row.apparentTempRaw?.let { temp ->
            score -= when {
                temp <= 5.0 -> PersonaConstants.RUN_PENALTY_TEMP_FREEZING
                temp <= 12.0 -> PersonaConstants.RUN_PENALTY_TEMP_CHILLY
                temp <= 24.0 -> 0
                temp <= 28.0 -> PersonaConstants.RUN_PENALTY_TEMP_WARM
                temp <= 32.0 -> PersonaConstants.RUN_PENALTY_TEMP_HOT
                temp <= 36.0 -> PersonaConstants.RUN_PENALTY_TEMP_VERY_HOT
                else -> PersonaConstants.RUN_PENALTY_TEMP_EXTREME
            }
        }
        row.precipProb?.let { rain ->
            score -= when {
                rain > 60 -> PersonaConstants.RUN_PENALTY_RAIN_HEAVY
                rain >= 30 -> PersonaConstants.RUN_PENALTY_RAIN_MODERATE
                rain >= 10 -> PersonaConstants.RUN_PENALTY_RAIN_LIGHT
                else -> 0
            }
        }
        row.windKmh?.let { wind ->
            score -= when {
                wind > 40.0 -> PersonaConstants.RUN_PENALTY_WIND_GALE
                wind >= 25.0 -> PersonaConstants.RUN_PENALTY_WIND_STRONG
                wind >= 15.0 -> PersonaConstants.RUN_PENALTY_WIND_MODERATE
                else -> 0
            }
        }
        row.aqi?.let { aqi ->
            score -= when {
                aqi > 150 -> PersonaConstants.RUN_PENALTY_AQI_HAZARDOUS
                aqi > 100 -> PersonaConstants.RUN_PENALTY_AQI_UNHEALTHY
                aqi > 50 -> PersonaConstants.RUN_PENALTY_AQI_MODERATE
                else -> 0
            }
        }
        if (row.isDay) {
            row.uvIndex?.let { uv ->
                score -= when {
                    uv >= 8.0 -> PersonaConstants.RUN_PENALTY_UV_VERY_HIGH
                    uv >= 6.0 -> PersonaConstants.RUN_PENALTY_UV_HIGH
                    else -> 0
                }
            }
        }
        row.visibilityM?.let { vis ->
            if (vis < 1000.0) score -= PersonaConstants.RUN_PENALTY_LOW_VISIBILITY
        }
        return score.coerceIn(0, 100)
    }

    private fun deduceRunningFailureReason(hours: List<HourlyRowUi>): String {
        if (hours.any { it.weatherCode in listOf(95, 96, 99) }) return "Thunderstorms expected"
        if (hours.all { (it.apparentTempRaw ?: 0.0) > 34.0 }) return "Excessive heat today"
        if (hours.all { (it.precipProb ?: 0) >= 60 }) return "Heavy rain throughout the day"
        if (hours.all { (it.aqi ?: 0) > 150 }) return "Unhealthy air quality today"
        if (hours.all { (it.windKmh ?: 0.0) > 40.0 }) return "Very strong winds"
        return "Unfavorable weather conditions"
    }

    private fun extractRunningReasons(first: HourlyRowUi, second: HourlyRowUi): List<String> {
        val list = mutableListOf<String>()
        val maxRain = maxOf(first.precipProb ?: 0, second.precipProb ?: 0)
        val avgTemp = ((first.apparentTempRaw ?: 25.0) + (second.apparentTempRaw ?: 25.0)) / 2.0
        val maxAqi = maxOf(first.aqi ?: 50, second.aqi ?: 50)
        val maxWind = maxOf(first.windKmh ?: 10.0, second.windKmh ?: 10.0)

        if (avgTemp in 14.0..24.0) list.add("cooler air")
        if (maxRain < 20) list.add("low rain chance")
        if (maxAqi <= 50) list.add("good air quality")
        if (list.size < 2 && maxWind <= 15.0) list.add("calm wind")
        if (list.isEmpty()) list.add("best available conditions")
        return list.take(2)
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

