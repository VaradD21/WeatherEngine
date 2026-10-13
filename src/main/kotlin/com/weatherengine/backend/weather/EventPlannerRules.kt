package com.weatherengine.backend.weather

import kotlin.math.roundToInt

object EventPlannerRules {

    // ── Extended Forecast Card ────────────────────────────────────────────────
    fun computeExtendedForecastCard(daily: OpenMeteoDailyForecast?): Map<String, Any?> {
        val days = daily?.time?.size?.coerceAtMost(EventPlannerConstants.FORECAST_DAYS) ?: 0
        val forecast = (0 until days).map { i ->
            mapOf(
                "date" to (daily?.time?.getOrNull(i) ?: ""),
                "condition" to wmoCondition(daily?.weatherCode?.getOrNull(i) ?: 0),
                "maxTempC" to (daily?.temperature2mMax?.getOrNull(i) ?: 0.0),
                "minTempC" to (daily?.temperature2mMin?.getOrNull(i) ?: 0.0),
                "precipProbPercent" to (daily?.precipitationProbabilityMax?.getOrNull(i) ?: 0)
            )
        }

        return mapOf(
            "days" to forecast,
            "forecastDayCount" to days,
            "message" to "${days}-day extended forecast available"
        )
    }

    // ── Rain Probability Card (afternoon focus) ───────────────────────────────
    fun computeRainProbabilityCard(
        hourly: OpenMeteoHourlyForecast?,
        daily: OpenMeteoDailyForecast?
    ): Map<String, Any?> {
        // Focus on event hours (3 PM – 7 PM) for the next 3 days
        val eventHourProbs = hourly?.time?.mapIndexedNotNull { idx, time ->
            val parsed = runCatching { java.time.LocalDateTime.parse(time) }.getOrNull()
            val hour = parsed?.hour ?: -1
            if (hour in EventPlannerConstants.EVENT_HOUR_START until EventPlannerConstants.EVENT_HOUR_END) {
                hourly.precipitationProbability?.getOrNull(idx)
            } else null
        }?.filterNotNull()?.take(15) ?: emptyList()    // 3 days × 5 event hours

        val peakProb = eventHourProbs.maxOrNull() ?: 0
        val avgProb = if (eventHourProbs.isNotEmpty()) eventHourProbs.average().roundToInt() else 0

        // 3-day daily max for supplemental context
        val daily3MaxProb = daily?.precipitationProbabilityMax
            ?.filterNotNull()?.take(3)?.maxOrNull() ?: peakProb

        val outlook = when {
            peakProb >= 70 -> "High rain probability during event hours — indoor backup strongly advised"
            peakProb >= EventPlannerConstants.RAIN_LIKELY_PERCENT ->
                "Moderate rain chance during event hours — have a contingency plan"
            peakProb >= 25 -> "Low rain risk — light showers possible but unlikely to disrupt events"
            else -> "Clear event windows — ideal outdoor conditions expected"
        }

        return mapOf(
            "peakEventHourProbPercent" to peakProb,
            "avgEventHourProbPercent" to avgProb,
            "daily3MaxProbPercent" to daily3MaxProb,
            "outlook" to outlook,
            "message" to outlook
        )
    }

    // ── Comfort Index Card ────────────────────────────────────────────────────
    fun computeComfortIndexCard(
        current: OpenMeteoCurrentWeather?,
        hourly: OpenMeteoHourlyForecast?,
        daily: OpenMeteoDailyForecast?
    ): Map<String, Any?> {
        val temp = current?.temperature2m ?: 22.0
        val humidity = current?.relativeHumidity2m ?: 50
        val uv = daily?.uvIndexMax?.firstOrNull() ?: hourly?.uvIndex?.firstOrNull { it != null } ?: 2.0
        val apparent = current?.apparentTemperature ?: temp

        // Score each dimension 0–100
        val tempScore = when {
            temp in EventPlannerConstants.TEMP_COMFORTABLE_MIN_C..EventPlannerConstants.TEMP_COMFORTABLE_MAX_C -> 100
            temp < 10.0 || temp > 38.0 -> 20
            temp < 15.0 || temp > 33.0 -> 50
            else -> 75
        }
        val humidityScore = when {
            humidity in 30..60 -> 100
            humidity in 20..70 -> 75
            humidity in 10..80 -> 50
            else -> 25
        }
        val uvScore = when {
            uv < 3.0 -> 100
            uv < 6.0 -> 80
            uv < 8.0 -> 55
            uv < 11.0 -> 30
            else -> 10
        }

        val comfortIndex = ((tempScore * 0.4) + (humidityScore * 0.35) + (uvScore * 0.25)).roundToInt()

        val (rating, advice) = when {
            comfortIndex >= EventPlannerConstants.COMFORT_GREAT_MIN ->
                "Excellent" to "Perfect outdoor conditions — guests will be very comfortable"
            comfortIndex >= EventPlannerConstants.COMFORT_GOOD_MIN ->
                "Good" to "Comfortable conditions — minor adjustments may improve comfort"
            comfortIndex >= EventPlannerConstants.COMFORT_FAIR_MIN ->
                "Fair" to "Conditions are acceptable but provide shade and cooling stations"
            else ->
                "Poor" to "Uncomfortable conditions — strongly consider indoor venue or date change"
        }

        return mapOf(
            "comfortIndex" to comfortIndex,
            "rating" to rating,
            "tempCelsius" to temp,
            "apparentTempCelsius" to apparent,
            "humidityPercent" to humidity,
            "uvIndex" to uv,
            "advice" to advice,
            "message" to "$rating outdoor comfort ($comfortIndex/100) — $advice"
        )
    }

    // ── WMO code → readable label ─────────────────────────────────────────────
    private fun wmoCondition(code: Int): String = when (code) {
        0 -> "Clear Sky"
        in 1..3 -> "Partly Cloudy"
        in 45..48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Rain Showers"
        in 95..99 -> "Thunderstorm"
        else -> "Variable"
    }
}
