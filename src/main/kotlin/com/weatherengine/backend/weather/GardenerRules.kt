package com.weatherengine.backend.weather

import kotlin.math.roundToInt

object GardenerRules {

    // ── Soil Moisture Card ────────────────────────────────────────────────────
    fun computeSoilMoistureCard(
        daily: OpenMeteoDailyForecast?,
        current: OpenMeteoCurrentWeather?
    ): Map<String, Any?> {
        val precip7d = daily?.precipitationSum
            ?.filterNotNull()
            ?.take(GardenerConstants.FORECAST_DAYS)
            ?.sum() ?: 0.0
        val humidity = current?.relativeHumidity2m ?: 50

        val (level, advice) = when {
            precip7d >= GardenerConstants.SOIL_WET_PRECIP_7D_MM ->
                "Wet" to "Soil is saturated — delay watering and check drainage"
            precip7d >= GardenerConstants.SOIL_MOIST_PRECIP_7D_MM ->
                "Moist" to "Soil moisture is adequate — hold off on irrigation"
            humidity < GardenerConstants.SOIL_DRY_HUMIDITY_MAX ->
                "Dry" to "Low humidity and little rain — irrigate soon"
            else ->
                "Normal" to "Soil moisture levels appear normal for the season"
        }

        return mapOf(
            "level" to level,
            "precip7dMm" to precip7d.roundTo1(),
            "humidityPercent" to humidity,
            "advice" to advice,
            "message" to "$level soil — $advice"
        )
    }

    // ── Rainfall Prediction Card ──────────────────────────────────────────────
    fun computeRainfallPredictionCard(daily: OpenMeteoDailyForecast?): Map<String, Any?> {
        val days = daily?.time?.size?.coerceAtMost(GardenerConstants.FORECAST_DAYS) ?: 0
        val totalMm = daily?.precipitationSum
            ?.filterNotNull()?.take(days)?.sum()?.roundTo1() ?: 0.0
        val maxProbability = daily?.precipitationProbabilityMax
            ?.filterNotNull()?.take(days)?.maxOrNull() ?: 0
        val rainyDayCount = daily?.precipitationSum
            ?.filterNotNull()?.take(days)?.count { it > 1.0 } ?: 0

        val outlook = when {
            rainyDayCount >= 5 -> "Very Wet — heavy rainfall expected most days"
            rainyDayCount >= 3 -> "Rainy Week — significant precipitation ahead"
            rainyDayCount >= 1 -> "Some Rain — a few showers in the forecast"
            else -> "Dry Spell — little to no rain expected this week"
        }

        return mapOf(
            "totalMm" to totalMm,
            "maxProbabilityPercent" to maxProbability,
            "rainyDayCount" to rainyDayCount,
            "forecastDays" to days,
            "outlook" to outlook,
            "message" to outlook
        )
    }

    // ── Frost Alert Card ──────────────────────────────────────────────────────
    fun computeFrostAlertCard(daily: OpenMeteoDailyForecast?): Map<String, Any?> {
        val minTemps = daily?.temperature2mMin?.filterNotNull()
            ?.take(GardenerConstants.FORECAST_DAYS)
            ?: emptyList()

        val hardFrostDays = minTemps.indices
            .filter { (minTemps.getOrNull(it) ?: 99.0) <= GardenerConstants.HARD_FROST_TEMP_C }
        val frostRiskDays = minTemps.indices
            .filter { (minTemps.getOrNull(it) ?: 99.0) <= GardenerConstants.FROST_RISK_TEMP_C }
            .filter { it !in hardFrostDays }

        val (alertLevel, advice) = when {
            hardFrostDays.isNotEmpty() -> {
                val dayIdx = hardFrostDays.first()
                val temp = minTemps.getOrNull(dayIdx)?.roundTo1() ?: GardenerConstants.HARD_FROST_TEMP_C
                "Hard Frost" to "Hard frost (${temp}°C) in ${daysFromNow(dayIdx)} — protect crops and cover sensitive plants immediately"
            }
            frostRiskDays.isNotEmpty() -> {
                val dayIdx = frostRiskDays.first()
                val temp = minTemps.getOrNull(dayIdx)?.roundTo1() ?: GardenerConstants.FROST_RISK_TEMP_C
                "Frost Risk" to "Frost risk (${temp}°C) in ${daysFromNow(dayIdx)} — cover tender plants overnight"
            }
            else -> "Clear" to "No frost risk detected in the next ${GardenerConstants.FORECAST_DAYS} days"
        }

        return mapOf(
            "alertActive" to (alertLevel != "Clear"),
            "alertLevel" to alertLevel,
            "frostRiskDayCount" to frostRiskDays.size,
            "hardFrostDayCount" to hardFrostDays.size,
            "advice" to advice,
            "message" to advice
        )
    }

    // ── Planting Guidance Card ────────────────────────────────────────────────
    fun computePlantingGuidanceCard(
        daily: OpenMeteoDailyForecast?,
        current: OpenMeteoCurrentWeather?
    ): Map<String, Any?> {
        val maxTemps = daily?.temperature2mMax?.filterNotNull()?.take(7) ?: emptyList()
        val minTemps = daily?.temperature2mMin?.filterNotNull()?.take(7) ?: emptyList()
        val totalRain = daily?.precipitationSum?.filterNotNull()?.take(7)?.sum() ?: 0.0
        val hasFrost = minTemps.any { it <= GardenerConstants.FROST_RISK_TEMP_C }
        val avgMax = if (maxTemps.isNotEmpty()) maxTemps.average() else 20.0
        val avgMin = if (minTemps.isNotEmpty()) minTemps.average() else 10.0

        val guidance = when {
            hasFrost -> "Avoid planting — frost risk detected in the next 7 days"
            avgMax > GardenerConstants.PLANT_TEMP_MAX_C ->
                "Mulch and water frequently — high heat may stress seedlings"
            avgMin < GardenerConstants.PLANT_TEMP_MIN_C ->
                "Soil may be too cold — wait for consistently warmer nights before sowing"
            totalRain > 50.0 ->
                "Saturated soil likely — delay planting until it dries out"
            avgMin in GardenerConstants.PLANT_TEMP_MIN_C..18.0 && totalRain in 5.0..30.0 ->
                "Good planting conditions — moderate temps and adequate moisture"
            else ->
                "Planting conditions are acceptable — monitor soil moisture daily"
        }

        return mapOf(
            "hasFrostRisk" to hasFrost,
            "avgMaxTempC" to avgMax.roundTo1(),
            "avgMinTempC" to avgMin.roundTo1(),
            "totalRainMm7d" to totalRain.roundTo1(),
            "guidance" to guidance,
            "message" to guidance
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun daysFromNow(dayIndex: Int) = when (dayIndex) {
        0 -> "tonight"
        1 -> "tomorrow"
        else -> "in $dayIndex days"
    }

    private fun Double.roundTo1() = (this * 10).roundToInt() / 10.0
}
