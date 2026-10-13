package com.weatherengine.backend.weather

import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

@Component
class WidgetBuilder {

    fun buildWidget(
        widgetCode: String,
        bundle: WeatherBundleDto,
        schoolStart: String = "08:00",
        schoolEnd: String = "15:00"
    ): WidgetDto {
        val current = bundle.forecast.current
        val hourly = bundle.forecast.hourly
        val daily = bundle.forecast.daily
        val aqiCurrent = bundle.airQuality?.current

        val data: Map<String, Any?> = when (widgetCode) {
            "aqi_card" -> {
                val usAqi = aqiCurrent?.usAqi
                if (usAqi != null && usAqi >= 0) {
                    mapOf("aqi" to usAqi, "category" to aqiCategory(usAqi), "pm25" to aqiCurrent.pm25)
                } else {
                    mapOf("status" to "unavailable", "message" to "Air quality station data unavailable for this area")
                }
            }
            "humidity_card" -> {
                val humidity = current?.relativeHumidity2m ?: 50
                mapOf("humidityPercent" to humidity.coerceIn(0, 100))
            }
            "uv_index_card" -> {
                val uvNow = hourly?.uvIndex?.firstOrNull { it != null } ?: daily?.uvIndexMax?.firstOrNull()
                if (uvNow != null) {
                    mapOf("uvIndex" to uvNow, "message" to uvAdvice(uvNow))
                } else {
                    mapOf("status" to "unavailable", "message" to "UV sensor index currently offline")
                }
            }
            "sunrise_sunset_card" -> {
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                OutdoorFitnessRules.computeSunriseSunsetCard(daily, nowLocal)
            }
            "wind_speed_card" -> {
                OutdoorFitnessRules.computeWindSpeedCard(current)
            }
            "heat_alert_card" -> {
                OutdoorFitnessRules.computeHeatAlertCard(current, hourly)
            }
            "traffic_card" -> {
                val precipProb = hourly?.precipitationProbability?.filterNotNull()?.take(6)?.maxOrNull() ?: 0
                val msg = when {
                    precipProb >= 60 -> "Rain expected (${precipProb}% chance) — allow extra commute time"
                    precipProb >= 30 -> "Light showers possible (${precipProb}% chance) during upcoming commute"
                    else -> "Dry road conditions expected for your commute"
                }
                mapOf("status" to "active", "message" to msg)
            }
            "visibility_card" -> {
                val visMeters = hourly?.visibility?.firstOrNull { it != null }?.roundToInt() ?: 10000
                val category = when {
                    visMeters >= 9000 -> "Clear"
                    visMeters >= 4000 -> "Moderate"
                    visMeters >= 1000 -> "Low"
                    else -> "Dense Fog"
                }
                mapOf("visibilityMeters" to visMeters, "category" to category)
            }
            "storm_fog_alert_card" -> {
                val code = current?.weatherCode ?: 0
                val stormOrFog = code in setOf(45, 48, 95, 96, 99) || code in 65..67 || code in 82..86
                val msg = if (stormOrFog) {
                    "Active weather advisory (WMO code $code) — exercise caution outdoors"
                } else {
                    "No storm or fog warnings in effect"
                }
                mapOf("alertActive" to stormOrFog, "conditionCode" to code, "message" to msg)
            }
            "pollen_card" -> {
                val top = listOfNotNull(
                    aqiCurrent?.grassPollen?.let { "Grass" to it },
                    aqiCurrent?.birchPollen?.let { "Birch" to it },
                    aqiCurrent?.alderPollen?.let { "Alder" to it },
                    aqiCurrent?.ragweedPollen?.let { "Ragweed" to it }
                ).maxByOrNull { it.second }
                if (top != null && top.second > 0.0) {
                    val count = top.second.roundToInt()
                    val level = when {
                        count >= 90 -> "Very High"
                        count >= 50 -> "High"
                        count >= 20 -> "Moderate"
                        else -> "Low"
                    }
                    mapOf(
                        "status" to "ok",
                        "pollenType" to top.first,
                        "grainsCount" to count,
                        "level" to level,
                        "message" to "${top.first} pollen is $level ($count grains/m³)"
                    )
                } else {
                    mapOf(
                        "status" to "ok",
                        "level" to "Low",
                        "message" to "Pollen levels are low or minimal for this region"
                    )
                }
            }
            "best_running_hours_card" -> {
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                OutdoorFitnessRules.computeBestRunningHours(hourly, nowLocal)
            }
            "wave_height_card" -> {
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                BeachgoerRules.computeWaveHeightCard(bundle.marine, nowLocal)
            }
            "sea_temp_card" -> {
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                BeachgoerRules.computeSeaTempCard(bundle.marine, current, nowLocal)
            }
            "tide_card" -> {
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                BeachgoerRules.computeTideCard(bundle.marine, nowLocal)
            }
            "school_commute_card" -> {
                val (start, end) = try {
                    ParentPersonaRules.parseAndValidateSchoolHours(schoolStart, schoolEnd)
                } catch (ex: IllegalArgumentException) {
                    Pair(java.time.LocalTime.of(8, 0), java.time.LocalTime.of(15, 0))
                }
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                val runWindow = ParentPersonaRules.nextSchoolRun(nowLocal, start, end)
                ParentPersonaRules.computeSchoolCommuteCard(hourly, runWindow)
            }
            "rain_alert_card" -> {
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                ParentPersonaRules.computeRainAlertCard(hourly, nowLocal)
            }
            "severe_weather_card" -> {
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                ParentPersonaRules.computeSevereWeatherCard(hourly, nowLocal)
            }
            "saved_destinations_card" -> {
                val lat = bundle.forecast.latitude ?: 0.0
                val lon = bundle.forecast.longitude ?: 0.0
                TravelerRules.computeSavedDestinationsCard(lat, lon)
            }
            "flight_alert_card" -> {
                val timezone = bundle.forecast.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
                val nowLocal = LocalDateTime.now(timezone)
                TravelerRules.computeFlightAlertCard(current, hourly, nowLocal)
            }
            "packing_suggestion_card" -> {
                TravelerRules.computePackingSuggestionCard(current, daily, hourly)
            }
            // ── Gardener ──────────────────────────────────────────────────────
            "soil_moisture_card" -> GardenerRules.computeSoilMoistureCard(daily, current)
            "rainfall_prediction_card" -> GardenerRules.computeRainfallPredictionCard(daily)
            "frost_alert_card" -> GardenerRules.computeFrostAlertCard(daily)
            "planting_guidance_card" -> GardenerRules.computePlantingGuidanceCard(daily, current)
            // ── Event Planner ─────────────────────────────────────────────────
            "extended_forecast_card" -> EventPlannerRules.computeExtendedForecastCard(daily)
            "rain_probability_card" -> EventPlannerRules.computeRainProbabilityCard(hourly, daily)
            "comfort_index_card" -> EventPlannerRules.computeComfortIndexCard(current, hourly, daily)
            else -> mapOf("status" to "unavailable", "message" to "Widget $widgetCode is ready")
        }

        return WidgetDto(type = widgetCode, data = data)
    }

    private fun aqiCategory(usAqi: Int): String = when {
        usAqi <= 50 -> "Good"
        usAqi <= 100 -> "Moderate"
        usAqi <= 150 -> "Unhealthy for Sensitive Groups"
        usAqi <= 200 -> "Unhealthy"
        usAqi <= 300 -> "Very Unhealthy"
        else -> "Hazardous"
    }

    private fun uvAdvice(uv: Double): String = when {
        uv < 3.0 -> "Low UV — minimal protection needed"
        uv < 6.0 -> "Moderate UV — wear sunglasses and SPF 30+"
        uv < 8.0 -> "High UV — seek shade during midday hours"
        uv < 11.0 -> "Very High UV — extra sun protection required"
        else -> "Extreme UV — avoid direct midday sun"
    }
}
