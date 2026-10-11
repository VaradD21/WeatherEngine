package com.weatherengine.backend.weather

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class RunningCandidate(
    val timeIso: String,
    val score: Int,
    val apparentTemp: Double?,
    val precipProb: Int?,
    val windKmh: Double?,
    val aqi: Int?
)

object OutdoorFitnessRules {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h a")

    fun computeSunriseSunsetCard(
        daily: OpenMeteoDailyForecast?,
        nowLocal: LocalDateTime
    ): Map<String, Any?> {
        val sunriseIso = daily?.sunrise?.firstOrNull()
        val sunsetIso = daily?.sunset?.firstOrNull()

        if (sunriseIso.isNullOrBlank() || sunsetIso.isNullOrBlank()) {
            return mapOf("status" to "unavailable", "message" to "Sunrise and sunset times unavailable")
        }

        val sunrise = runCatching { LocalDateTime.parse(sunriseIso) }.getOrNull()
        val sunset = runCatching { LocalDateTime.parse(sunsetIso) }.getOrNull()

        if (sunrise == null || sunset == null) {
            return mapOf(
                "status" to "ok",
                "sunrise" to sunriseIso,
                "sunset" to sunsetIso,
                "daylightState" to "Daylight",
                "message" to "Sunrise: $sunriseIso • Sunset: $sunsetIso"
            )
        }

        val dayLengthMin = Duration.between(sunrise, sunset).toMinutes().coerceAtLeast(0)
        val dayLengthFormatted = "${dayLengthMin / 60}h ${dayLengthMin % 60}m"
        val sunriseFormatted = sunrise.format(DateTimeFormatter.ofPattern("hh:mm a"))
        val sunsetFormatted = sunset.format(DateTimeFormatter.ofPattern("hh:mm a"))

        val (state, timeUntil, msg) = when {
            nowLocal.isBefore(sunrise) -> {
                val untilMin = Duration.between(nowLocal, sunrise).toMinutes().coerceAtLeast(0)
                val untilStr = "${untilMin / 60}h ${untilMin % 60}m"
                Triple("BeforeSunrise", untilStr, "$untilStr until sunrise ($sunriseFormatted)")
            }
            nowLocal.isBefore(sunset) -> {
                val untilMin = Duration.between(nowLocal, sunset).toMinutes().coerceAtLeast(0)
                val untilStr = "${untilMin / 60}h ${untilMin % 60}m"
                Triple("Daylight", untilStr, "$untilStr of daylight remaining until sunset ($sunsetFormatted)")
            }
            else -> {
                Triple("AfterSunset", "0m", "Sunset was at $sunsetFormatted (Day length: $dayLengthFormatted)")
            }
        }

        return mapOf(
            "status" to "ok",
            "sunrise" to sunriseFormatted,
            "sunset" to sunsetFormatted,
            "daylightState" to state,
            "dayLength" to dayLengthFormatted,
            "timeUntil" to timeUntil,
            "message" to msg
        )
    }

    fun computeBestRunningHours(
        hourly: OpenMeteoHourlyForecast?,
        nowLocal: LocalDateTime
    ): Map<String, Any?> {
        if (hourly == null || hourly.time.isEmpty()) {
            return mapOf("status" to "unavailable", "message" to "Hourly forecast unavailable for workout planning")
        }

        val n = hourly.time.size
        val rows = (0 until n).mapNotNull { i ->
            val timeStr = hourly.time.getOrNull(i) ?: return@mapNotNull null
            val time = runCatching { LocalDateTime.parse(timeStr) }.getOrNull() ?: return@mapNotNull null
            if (time.isBefore(nowLocal.truncatedTo(java.time.temporal.ChronoUnit.HOURS))) return@mapNotNull null

            val hourOfDay = time.hour
            if (hourOfDay !in OutdoorFitnessConstants.RUN_CANDIDATE_START_HOUR..OutdoorFitnessConstants.RUN_CANDIDATE_END_HOUR) {
                return@mapNotNull null
            }

            val apparent = hourly.apparentTemperature.getOrNull(i)
            val precip = hourly.precipitationProbability.getOrNull(i)
            val wind = hourly.windSpeed10m.getOrNull(i)
            val code = hourly.weatherCode.getOrNull(i)

            val score = scoreRunningHour(apparent, precip, wind, code)
            RunningCandidate(timeStr, score, apparent, precip, wind, null)
        }.take(24)

        if (rows.size < OutdoorFitnessConstants.RUN_WINDOW_CONSECUTIVE_HOURS) {
            return mapOf(
                "status" to "ok",
                "hasOptimalWindow" to false,
                "message" to "Not enough daylight hours remaining today for outdoor running"
            )
        }

        var bestAvg = -1.0
        var bestIdx = 0
        for (i in 0..rows.size - OutdoorFitnessConstants.RUN_WINDOW_CONSECUTIVE_HOURS) {
            val avg = (rows[i].score + rows[i + 1].score) / 2.0
            if (avg > bestAvg) {
                bestAvg = avg
                bestIdx = i
            }
        }

        val finalScore = bestAvg.roundToInt()
        if (finalScore < OutdoorFitnessConstants.RUN_MIN_ACCEPTABLE_SCORE) {
            val failureReason = deduceFailureReason(rows)
            return mapOf(
                "status" to "ok",
                "hasOptimalWindow" to false,
                "score" to finalScore,
                "message" to "No ideal running window ($failureReason)"
            )
        }

        val first = rows[bestIdx]
        val second = rows[bestIdx + 1]
        val t1 = LocalDateTime.parse(first.timeIso)
        val t2 = LocalDateTime.parse(second.timeIso).plusHours(1)
        val windowLabel = "${t1.format(TIME_FORMATTER)} – ${t2.format(TIME_FORMATTER)}"

        val reasons = mutableListOf<String>()
        first.apparentTemp?.let { reasons.add("Feels like ${it.roundToInt()}°C") }
        if ((first.precipProb ?: 0) < 20) reasons.add("Dry conditions")
        if ((first.windKmh ?: 0.0) < 20.0) reasons.add("Gentle breeze")

        return mapOf(
            "status" to "ok",
            "hasOptimalWindow" to true,
            "window" to windowLabel,
            "score" to finalScore,
            "reasons" to reasons,
            "message" to "Optimal running window: $windowLabel (Score: $finalScore/100)"
        )
    }

    private fun scoreRunningHour(
        apparent: Double?,
        precip: Int?,
        wind: Double?,
        code: Int?
    ): Int {
        if (code in ParentPersonaConstants.THUNDERSTORM_CODES) return 0

        var score = 100
        apparent?.let { temp ->
            score -= when {
                temp <= 5.0 -> OutdoorFitnessConstants.PENALTY_TEMP_FREEZING
                temp <= 12.0 -> OutdoorFitnessConstants.PENALTY_TEMP_CHILLY
                temp <= 24.0 -> 0
                temp <= 28.0 -> OutdoorFitnessConstants.PENALTY_TEMP_WARM
                temp <= 32.0 -> OutdoorFitnessConstants.PENALTY_TEMP_HOT
                temp <= 36.0 -> OutdoorFitnessConstants.PENALTY_TEMP_VERY_HOT
                else -> OutdoorFitnessConstants.PENALTY_TEMP_EXTREME
            }
        }
        precip?.let { rain ->
            score -= when {
                rain > 60 -> OutdoorFitnessConstants.PENALTY_RAIN_HEAVY
                rain >= 30 -> OutdoorFitnessConstants.PENALTY_RAIN_MODERATE
                rain >= 10 -> OutdoorFitnessConstants.PENALTY_RAIN_LIGHT
                else -> 0
            }
        }
        wind?.let { w ->
            score -= when {
                w > 40.0 -> OutdoorFitnessConstants.PENALTY_WIND_GALE
                w >= 25.0 -> OutdoorFitnessConstants.PENALTY_WIND_STRONG
                w >= 15.0 -> OutdoorFitnessConstants.PENALTY_WIND_MODERATE
                else -> 0
            }
        }
        return score.coerceIn(0, 100)
    }

    private fun deduceFailureReason(candidates: List<RunningCandidate>): String {
        val maxTemp = candidates.mapNotNull { it.apparentTemp }.maxOrNull() ?: 25.0
        val maxRain = candidates.mapNotNull { it.precipProb }.maxOrNull() ?: 0
        val maxWind = candidates.mapNotNull { it.windKmh }.maxOrNull() ?: 0.0

        return when {
            maxRain > 60 -> "Heavy rain expected"
            maxTemp >= 35.0 -> "Excessive heat (feels like ${maxTemp.roundToInt()}°C)"
            maxTemp <= 5.0 -> "Near freezing temperatures"
            maxWind > 35.0 -> "Strong gusty winds"
            else -> "Suboptimal weather conditions"
        }
    }

    fun computeWindSpeedCard(
        current: OpenMeteoCurrentWeather?
    ): Map<String, Any?> {
        val speedKmh = current?.windSpeed10m ?: 0.0
        val gustsKmh = current?.windGusts10m ?: speedKmh
        val speedMs = ((speedKmh / 3.6) * 10.0).roundToInt() / 10.0

        val category = when {
            speedKmh <= OutdoorFitnessConstants.WIND_LIGHT_MAX -> "Light Breeze"
            speedKmh <= OutdoorFitnessConstants.WIND_MODERATE_MAX -> "Moderate Wind"
            speedKmh <= OutdoorFitnessConstants.WIND_BREEZY_MAX -> "Strong Breeze"
            else -> "High Wind / Gale"
        }

        val advice = when {
            speedKmh > 35.0 -> "High headwind — challenging for running/cycling"
            speedKmh > 20.0 -> "Noticeable resistance outdoors"
            else -> "Great calm conditions for running or cycling"
        }

        return mapOf(
            "status" to "ok",
            "speedKmh" to speedKmh,
            "speedMetersPerSecond" to speedMs,
            "gustsKmh" to gustsKmh,
            "category" to category,
            "message" to "$speedKmh km/h ($category) • $advice"
        )
    }

    fun computeHeatAlertCard(
        current: OpenMeteoCurrentWeather?,
        hourly: OpenMeteoHourlyForecast?
    ): Map<String, Any?> {
        val temp = current?.apparentTemperature ?: current?.temperature2m ?: 25.0
        val maxApparent = hourly?.apparentTemperature?.filterNotNull()?.take(24)?.maxOrNull() ?: temp
        val alertActive = maxApparent >= OutdoorFitnessConstants.HEAT_CAUTION_MIN

        val (severity, msg) = when {
            maxApparent >= OutdoorFitnessConstants.HEAT_ALERT_MIN -> {
                Pair("Warning", "Extreme Heat Alert: Feels like ${maxApparent.roundToInt()}°C. Avoid strenuous outdoor workouts during midday.")
            }
            maxApparent >= OutdoorFitnessConstants.HEAT_CAUTION_MIN -> {
                Pair("Caution", "Heat Caution: Feels like ${maxApparent.roundToInt()}°C. Stay hydrated and schedule workouts early morning or evening.")
            }
            else -> {
                Pair("Normal", "No heat risks today. Comfortable workout temperatures.")
            }
        }

        return mapOf(
            "status" to "ok",
            "alertActive" to alertActive,
            "severity" to severity,
            "temperatureCelsius" to temp,
            "maxApparentCelsius" to maxApparent,
            "disclaimer" to OutdoorFitnessConstants.HEAT_DISCLAIMER_NOTE,
            "message" to msg
        )
    }
}
