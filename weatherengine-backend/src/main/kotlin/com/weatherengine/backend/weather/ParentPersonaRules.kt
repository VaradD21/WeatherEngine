package com.weatherengine.backend.weather

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

data class SchoolRunWindow(
    val run: String, // "morning" or "afternoon"
    val date: LocalDate,
    val windowStart: LocalDateTime,
    val windowEnd: LocalDateTime
) {
    val windowLabel: String
        get() {
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            return "${windowStart.format(fmt)} - ${windowEnd.format(fmt)}"
        }
    val dateLabel: String
        get() = date.toString()
}

data class SevereAlert(
    val type: String,
    val startsAt: String,
    val message: String
)

object ParentPersonaRules {

    private val TIME_FORMATTER_24 = DateTimeFormatter.ofPattern("HH:mm")

    fun parseAndValidateSchoolHours(startStr: String, endStr: String): Pair<LocalTime, LocalTime> {
        val start = try {
            val t = LocalTime.parse(startStr.trim(), TIME_FORMATTER_24)
            if (startStr.trim() != t.format(TIME_FORMATTER_24)) throw IllegalArgumentException()
            t
        } catch (_: Exception) {
            throw IllegalArgumentException("schoolStart must be in HH:mm 24-hour format")
        }

        val end = try {
            val t = LocalTime.parse(endStr.trim(), TIME_FORMATTER_24)
            if (endStr.trim() != t.format(TIME_FORMATTER_24)) throw IllegalArgumentException()
            t
        } catch (_: Exception) {
            throw IllegalArgumentException("schoolEnd must be in HH:mm 24-hour format")
        }

        if (!end.isAfter(start)) {
            throw IllegalArgumentException("schoolEnd must be strictly after schoolStart")
        }

        return Pair(start, end)
    }

    fun nextSchoolRun(nowLocal: LocalDateTime, start: LocalTime, end: LocalTime): SchoolRunWindow {
        require(end.isAfter(start)) { "schoolEnd must be strictly after schoolStart" }

        var candidateDate = nowLocal.toLocalDate()

        // If today is a weekend, roll forward to Monday morning
        if (candidateDate.dayOfWeek == DayOfWeek.SATURDAY) {
            candidateDate = candidateDate.plusDays(2)
            return makeMorningWindow(candidateDate, start)
        } else if (candidateDate.dayOfWeek == DayOfWeek.SUNDAY) {
            candidateDate = candidateDate.plusDays(1)
            return makeMorningWindow(candidateDate, start)
        }

        // Today is Mon-Fri
        val morningEnd = candidateDate.atTime(start)
        if (morningEnd.isAfter(nowLocal)) {
            return makeMorningWindow(candidateDate, start)
        }

        val afternoonEnd = candidateDate.atTime(end).plusMinutes(ParentPersonaConstants.RUN_WINDOW_DURATION_MINUTES)
        if (afternoonEnd.isAfter(nowLocal)) {
            return makeAfternoonWindow(candidateDate, end)
        }

        // Both windows today are in the past -> next school day morning
        val nextDay = when (candidateDate.dayOfWeek) {
            DayOfWeek.FRIDAY -> candidateDate.plusDays(3)
            else -> candidateDate.plusDays(1)
        }
        return makeMorningWindow(nextDay, start)
    }

    private fun makeMorningWindow(date: LocalDate, start: LocalTime): SchoolRunWindow {
        val wStart = date.atTime(start).minusMinutes(ParentPersonaConstants.RUN_WINDOW_DURATION_MINUTES)
        val wEnd = date.atTime(start)
        return SchoolRunWindow("morning", date, wStart, wEnd)
    }

    private fun makeAfternoonWindow(date: LocalDate, end: LocalTime): SchoolRunWindow {
        val wStart = date.atTime(end)
        val wEnd = date.atTime(end).plusMinutes(ParentPersonaConstants.RUN_WINDOW_DURATION_MINUTES)
        return SchoolRunWindow("afternoon", date, wStart, wEnd)
    }

    fun computeSchoolCommuteCard(
        hourly: OpenMeteoHourlyForecast?,
        window: SchoolRunWindow
    ): Map<String, Any?> {
        if (hourly == null || hourly.time.isEmpty()) {
            return mapOf("status" to "unavailable", "message" to "Hourly forecast unavailable")
        }

        val startHour = window.windowStart.truncatedTo(ChronoUnit.HOURS)
        val endHour = window.windowEnd.truncatedTo(ChronoUnit.HOURS)

        val overlappingIndices = mutableListOf<Int>()
        for (i in hourly.time.indices) {
            val tStr = hourly.time[i]
            val rowTime = try { LocalDateTime.parse(tStr) } catch (_: Exception) { null }
            if (rowTime != null) {
                val rowHour = rowTime.truncatedTo(ChronoUnit.HOURS)
                if (!rowHour.isBefore(startHour) && !rowHour.isAfter(endHour)) {
                    overlappingIndices.add(i)
                }
            }
        }

        if (overlappingIndices.isEmpty()) {
            return mapOf("status" to "unavailable", "message" to "Hourly forecast unavailable for commute window")
        }

        val rainProbs = overlappingIndices.mapNotNull { hourly.precipitationProbability.getOrNull(it) }
        val maxRainProb = rainProbs.maxOrNull()

        val precips = overlappingIndices.mapNotNull { hourly.precipitation.getOrNull(it) }
        val maxHourPrecip = precips.maxOrNull()
        val totalPrecip = if (precips.isNotEmpty()) precips.sum() else null

        val visibilities = overlappingIndices.mapNotNull { hourly.visibility.getOrNull(it) }
        val minVisibility = visibilities.minOrNull()

        val gusts = overlappingIndices.mapNotNull { hourly.windGusts10m.getOrNull(it) }
        val maxGusts = gusts.maxOrNull()

        val apparentTemps = overlappingIndices.mapNotNull { hourly.apparentTemperature.getOrNull(it) }
        val maxApparent = apparentTemps.maxOrNull()
        val minApparent = apparentTemps.minOrNull()

        val weatherCodes = overlappingIndices.mapNotNull { hourly.weatherCode.getOrNull(it) }
        val hasThunderstorm = weatherCodes.any { it in ParentPersonaConstants.THUNDERSTORM_CODES }

        val maxUv = if (window.run == "afternoon") {
            overlappingIndices.mapNotNull { hourly.uvIndex.getOrNull(it) }.maxOrNull()
        } else null

        // Verdict determination: worst applicable wins (Caution > Prepare > Good)
        val isCaution = hasThunderstorm ||
                (maxHourPrecip != null && maxHourPrecip >= ParentPersonaConstants.COMMUTE_CAUTION_PRECIP_MM) ||
                (maxGusts != null && maxGusts >= ParentPersonaConstants.COMMUTE_CAUTION_GUST_KMH) ||
                (minVisibility != null && minVisibility < ParentPersonaConstants.COMMUTE_CAUTION_VISIBILITY_METERS) ||
                (maxApparent != null && maxApparent >= ParentPersonaConstants.COMMUTE_CAUTION_APPARENT_HOT) ||
                (minApparent != null && minApparent <= ParentPersonaConstants.COMMUTE_CAUTION_APPARENT_COLD)

        val isPrepare = !isCaution && (
                (maxRainProb != null && maxRainProb >= ParentPersonaConstants.COMMUTE_PREPARE_RAIN_PROB) ||
                (totalPrecip != null && totalPrecip >= ParentPersonaConstants.COMMUTE_PREPARE_PRECIP_MM) ||
                (maxGusts != null && maxGusts >= ParentPersonaConstants.COMMUTE_PREPARE_GUST_KMH) ||
                (maxApparent != null && maxApparent >= ParentPersonaConstants.COMMUTE_PREPARE_APPARENT_HOT) ||
                (minApparent != null && minApparent <= ParentPersonaConstants.COMMUTE_PREPARE_APPARENT_COLD) ||
                (maxUv != null && maxUv >= ParentPersonaConstants.COMMUTE_PREPARE_UV_INDEX)
        )

        val verdict = when {
            isCaution -> "caution"
            isPrepare -> "prepare"
            else -> "good"
        }

        val reasons = mutableListOf<String>()
        val tips = mutableListOf<String>()

        if (hasThunderstorm) {
            reasons.add("Thunderstorm expected")
            tips.add("Carry rain gear and avoid outdoor delays")
        } else if (maxHourPrecip != null && maxHourPrecip >= ParentPersonaConstants.COMMUTE_CAUTION_PRECIP_MM) {
            reasons.add("Torrential rain (${maxHourPrecip} mm/h)")
            tips.add("Pack an umbrella or raincoat")
        } else if (maxRainProb != null && maxRainProb >= ParentPersonaConstants.COMMUTE_PREPARE_RAIN_PROB) {
            reasons.add("Rain likely (${maxRainProb}%)")
            tips.add("Pack an umbrella or raincoat")
        } else if (totalPrecip != null && totalPrecip >= ParentPersonaConstants.COMMUTE_PREPARE_PRECIP_MM) {
            val rounded = (totalPrecip * 10.0).roundToInt() / 10.0
            reasons.add("Precipitation expected (${rounded} mm)")
            tips.add("Pack an umbrella or raincoat")
        }

        if (maxGusts != null && maxGusts >= ParentPersonaConstants.COMMUTE_CAUTION_GUST_KMH) {
            reasons.add("Strong gusts ${maxGusts.roundToInt()} km/h")
        } else if (maxGusts != null && maxGusts >= ParentPersonaConstants.COMMUTE_PREPARE_GUST_KMH) {
            reasons.add("Gusts up to ${maxGusts.roundToInt()} km/h")
        }

        if (minVisibility != null && minVisibility < ParentPersonaConstants.COMMUTE_CAUTION_VISIBILITY_METERS) {
            reasons.add("Low visibility (${minVisibility.roundToInt()} m)")
            tips.add("Leave a bit earlier for low visibility")
        }

        if (maxApparent != null && maxApparent >= ParentPersonaConstants.COMMUTE_CAUTION_APPARENT_HOT) {
            reasons.add("Extreme heat (${maxApparent.roundToInt()}°C feels-like)")
            tips.add("Carry water and wear a sun hat")
        } else if (maxApparent != null && maxApparent >= ParentPersonaConstants.COMMUTE_PREPARE_APPARENT_HOT) {
            reasons.add("Warm weather (${maxApparent.roundToInt()}°C feels-like)")
            tips.add("Carry water and stay hydrated")
        }

        if (minApparent != null && minApparent <= ParentPersonaConstants.COMMUTE_CAUTION_APPARENT_COLD) {
            reasons.add("Freezing temp (${minApparent.roundToInt()}°C feels-like)")
            tips.add("Wear a heavy warm jacket")
        } else if (minApparent != null && minApparent <= ParentPersonaConstants.COMMUTE_PREPARE_APPARENT_COLD) {
            reasons.add("Chilly morning (${minApparent.roundToInt()}°C feels-like)")
            tips.add("Wear a warm jacket")
        }

        if (maxUv != null && maxUv >= ParentPersonaConstants.COMMUTE_PREPARE_UV_INDEX) {
            reasons.add("High UV index (${maxUv.roundToInt()})")
            if (!tips.contains("Carry water and wear a sun hat")) {
                tips.add("Wear sunscreen and a sun hat")
            }
        }

        if (verdict == "good") {
            reasons.add("Favorable weather for school commute")
            tips.add("Standard commute preparation")
        }

        return mapOf(
            "status" to "ok",
            "run" to window.run,
            "date" to window.dateLabel,
            "window" to window.windowLabel,
            "verdict" to verdict,
            "reasons" to reasons.take(3),
            "tips" to tips.distinct().take(3)
        )
    }

    fun computeRainAlertCard(
        hourly: OpenMeteoHourlyForecast?,
        nowLocal: LocalDateTime
    ): Map<String, Any?> {
        if (hourly == null || hourly.time.isEmpty()) {
            return mapOf("status" to "unavailable", "message" to "Hourly rain forecast unavailable")
        }

        val startHour = nowLocal.truncatedTo(ChronoUnit.HOURS)
        val windowIndices = mutableListOf<Int>()
        for (i in hourly.time.indices) {
            val tStr = hourly.time[i]
            val rowTime = try { LocalDateTime.parse(tStr) } catch (_: Exception) { null }
            if (rowTime != null && !rowTime.isBefore(startHour)) {
                windowIndices.add(i)
                if (windowIndices.size == ParentPersonaConstants.RAIN_ALERT_LOOKAHEAD_HOURS) break
            }
        }

        if (windowIndices.isEmpty()) {
            return mapOf("status" to "unavailable", "message" to "Hourly rain forecast unavailable")
        }

        var peakProb = 0
        var peakHourLabel: String? = null

        var firstHighRainHourLabel: String? = null

        val hourFormat = DateTimeFormatter.ofPattern("h a", Locale.US)

        for (idx in windowIndices) {
            val prob = hourly.precipitationProbability.getOrNull(idx) ?: 0
            val precip = hourly.precipitation.getOrNull(idx) ?: 0.0
            val tStr = hourly.time[idx]
            val time = try { LocalDateTime.parse(tStr) } catch (_: Exception) { null }
            val label = time?.format(hourFormat) ?: tStr

            if (prob > peakProb || peakHourLabel == null) {
                peakProb = prob
                peakHourLabel = label
            }

            if (firstHighRainHourLabel == null && (prob >= ParentPersonaConstants.RAIN_ALERT_HIGH_PROB || precip >= ParentPersonaConstants.RAIN_ALERT_HIGH_PRECIP_MM)) {
                firstHighRainHourLabel = label
            }
        }

        val message = when {
            firstHighRainHourLabel != null -> "Rain likely around $firstHighRainHourLabel"
            peakProb in ParentPersonaConstants.RAIN_ALERT_LIGHT_PROB_MIN..ParentPersonaConstants.RAIN_ALERT_LIGHT_PROB_MAX -> "Light chance of rain"
            else -> "No rain expected in the next 6 hours"
        }

        return mapOf(
            "status" to "ok",
            "message" to message,
            "peakProbabilityPercent" to peakProb,
            "peakHourLabel" to peakHourLabel
        )
    }

    fun computeSevereWeatherCard(
        hourly: OpenMeteoHourlyForecast?,
        nowLocal: LocalDateTime
    ): Map<String, Any?> {
        if (hourly == null || hourly.time.isEmpty()) {
            return mapOf("status" to "unavailable", "message" to "Severe weather data unavailable")
        }

        val startHour = nowLocal.truncatedTo(ChronoUnit.HOURS)
        val windowIndices = mutableListOf<Int>()
        for (i in hourly.time.indices) {
            val tStr = hourly.time[i]
            val rowTime = try { LocalDateTime.parse(tStr) } catch (_: Exception) { null }
            if (rowTime != null && !rowTime.isBefore(startHour)) {
                windowIndices.add(i)
                if (windowIndices.size == ParentPersonaConstants.SEVERE_LOOKAHEAD_HOURS) break
            }
        }

        if (windowIndices.isEmpty()) {
            return mapOf("status" to "unavailable", "message" to "Severe weather data unavailable")
        }

        val hourFormat = DateTimeFormatter.ofPattern("h a", Locale.US)
        fun formatHour(idx: Int): String {
            val tStr = hourly.time[idx]
            val time = try { LocalDateTime.parse(tStr) } catch (_: Exception) { null }
            return time?.format(hourFormat) ?: tStr
        }

        val alerts = mutableListOf<Map<String, String>>()

        // 1. Thunderstorm
        val firstThunderstorm = windowIndices.firstOrNull {
            hourly.weatherCode.getOrNull(it) in ParentPersonaConstants.THUNDERSTORM_CODES
        }
        if (firstThunderstorm != null) {
            alerts.add(mapOf("type" to "thunderstorm", "startsAt" to formatHour(firstThunderstorm), "message" to "Thunderstorm danger"))
        }

        // 2. Heavy rain
        val firstHeavyRain = windowIndices.firstOrNull {
            val precip = hourly.precipitation.getOrNull(it)
            val code = hourly.weatherCode.getOrNull(it)
            (precip != null && precip >= ParentPersonaConstants.SEVERE_HEAVY_RAIN_MM) || code in ParentPersonaConstants.SEVERE_HEAVY_RAIN_CODES
        }
        if (firstHeavyRain != null) {
            alerts.add(mapOf("type" to "heavy_rain", "startsAt" to formatHour(firstHeavyRain), "message" to "Heavy rain advisory"))
        }

        // 3. Strong wind
        val firstStrongWind = windowIndices.firstOrNull {
            val gusts = hourly.windGusts10m.getOrNull(it)
            gusts != null && gusts >= ParentPersonaConstants.SEVERE_WIND_GUSTS_KMH
        }
        if (firstStrongWind != null) {
            alerts.add(mapOf("type" to "strong_wind", "startsAt" to formatHour(firstStrongWind), "message" to "Dangerous wind gusts"))
        }

        // 4. Low visibility
        val firstLowVis = windowIndices.firstOrNull {
            val vis = hourly.visibility.getOrNull(it)
            val code = hourly.weatherCode.getOrNull(it)
            (vis != null && vis < ParentPersonaConstants.SEVERE_LOW_VISIBILITY_METERS) || code in ParentPersonaConstants.SEVERE_FOG_CODES
        }
        if (firstLowVis != null) {
            alerts.add(mapOf("type" to "low_visibility", "startsAt" to formatHour(firstLowVis), "message" to "Low visibility / dense fog"))
        }

        // 5. Extreme heat
        val firstHeat = windowIndices.firstOrNull {
            val apparent = hourly.apparentTemperature.getOrNull(it)
            apparent != null && apparent >= ParentPersonaConstants.SEVERE_EXTREME_HEAT_APPARENT
        }
        if (firstHeat != null) {
            alerts.add(mapOf("type" to "extreme_heat", "startsAt" to formatHour(firstHeat), "message" to "Extreme heat warning"))
        }

        val message = if (alerts.isNotEmpty()) {
            "${alerts.size} active alert(s) in next 24h. ${ParentPersonaConstants.SEVERE_DISCLAIMER}"
        } else {
            "No severe weather expected in the next 24 hours. ${ParentPersonaConstants.SEVERE_DISCLAIMER}"
        }

        return mapOf(
            "status" to "ok",
            "alerts" to alerts,
            "message" to message
        )
    }
}
