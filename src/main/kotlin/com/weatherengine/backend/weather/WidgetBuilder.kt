package com.weatherengine.backend.weather

import com.weatherengine.backend.location.LocationRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.roundToInt

@Component
class WidgetBuilder(
    private val locationRepository: LocationRepository? = null
) {

    fun buildWidget(
        widgetCode: String,
        bundle: WeatherBundleDto,
        schoolStart: String = "08:00",
        schoolEnd: String = "15:00",
        userId: UUID? = null
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
                mapOf("precipProbabilityNext6h" to precipProb, "roadCondition" to msg)
            }
            "visibility_card" -> {
                val visMeters = hourly?.visibility?.filterNotNull()?.firstOrNull() ?: 10000.0
                val visKm = ((visMeters / 100.0).roundToInt()) / 10.0
                val status = when {
                    visKm < 1.0 -> "Dense Fog"
                    visKm < 4.0 -> "Moderate Fog / Mist"
                    visKm < 10.0 -> "Hazy"
                    else -> "Clear Visibility"
                }
                mapOf("visibilityKm" to visKm, "status" to status)
            }
            "pollen_card" -> {
                val grass = aqiCurrent?.grassPollen ?: 0.0
                val birch = aqiCurrent?.birchPollen ?: 0.0
                val alder = aqiCurrent?.alderPollen ?: 0.0
                val ragweed = aqiCurrent?.ragweedPollen ?: 0.0
                val maxPollen = maxOf(grass, birch, alder, ragweed)
                val risk = when {
                    maxPollen >= 50.0 -> "HIGH"
                    maxPollen >= 15.0 -> "MODERATE"
                    else -> "LOW"
                }
                mapOf("overallRisk" to risk, "grass" to grass, "birch" to birch, "alder" to alder, "ragweed" to ragweed)
            }
            "storm_fog_alert_card" -> {
                val code = current?.weatherCode ?: 0
                val alert = when (code) {
                    in setOf(95, 96, 99) -> "Active Thunderstorm Warning in effect"
                    in setOf(45, 48) -> "Dense Fog Advisory — reduce driving speed"
                    in setOf(61, 63, 65, 80, 81, 82) -> "Heavy Precipitation Advisory"
                    else -> "No severe storm or fog alerts active"
                }
                mapOf("alertMessage" to alert, "weatherCode" to code)
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
                val savedLocations = if (userId != null && locationRepository != null) {
                    locationRepository.findByUserId(userId)
                } else emptyList()
                TravelerRules.computeSavedDestinationsCard(savedLocations)
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

    private fun aqiCategory(aqi: Int): String = when {
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Unhealthy for Sensitive Groups"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very Unhealthy"
        else -> "Hazardous"
    }

    private fun uvAdvice(uv: Double): String = when {
        uv >= 8 -> "Very High UV risk — minimize midday sun, use SPF 50+"
        uv >= 6 -> "High UV risk — seek shade, wear hat and sunglasses"
        uv >= 3 -> "Moderate UV risk — sunscreen recommended during midday"
        else -> "Low UV risk — minimal sun protection needed"
    }
}
