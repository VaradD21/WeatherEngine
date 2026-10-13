package com.weatherengine.app.core

sealed class WidgetUi(open val type: String) {
    data class Aqi(
        override val type: String = "aqi_card",
        val aqi: Int,
        val category: String
    ) : WidgetUi(type)

    data class Humidity(
        override val type: String = "humidity_card",
        val humidityPercent: Int
    ) : WidgetUi(type)

    data class Uv(
        override val type: String = "uv_index_card",
        val status: String?,
        val uvIndex: Double?,
        val message: String?
    ) : WidgetUi(type)

    data class Pollen(
        override val type: String = "pollen_card",
        val pollenType: String = "Pollen",
        val grainsCount: Int = 0,
        val level: String = "Low",
        val message: String
    ) : WidgetUi(type)

    data class SunriseSunset(
        override val type: String = "sunrise_sunset_card",
        val sunrise: String,
        val sunset: String,
        val daylightState: String = "Daylight",
        val dayLength: String = "",
        val timeUntil: String = "",
        val message: String = ""
    ) : WidgetUi(type)

    data class BestRunning(
        override val type: String = "best_running_hours_card",
        val hasOptimalWindow: Boolean = false,
        val window: String? = null,
        val score: Int? = null,
        val reasons: List<String> = emptyList(),
        val message: String = ""
    ) : WidgetUi(type)

    data class Wind(
        override val type: String = "wind_speed_card",
        val speedKmh: Double = 0.0,
        val speedMetersPerSecond: Double = 0.0,
        val gustsKmh: Double = 0.0,
        val category: String = "Moderate",
        val message: String = ""
    ) : WidgetUi(type)

    data class HeatAlert(
        override val type: String = "heat_alert_card",
        val alertActive: Boolean,
        val severity: String = "Normal",
        val temperatureCelsius: Double,
        val maxApparentCelsius: Double? = null,
        val message: String
    ) : WidgetUi(type)

    data class Traffic(
        override val type: String = "traffic_card",
        val status: String,
        val message: String
    ) : WidgetUi(type)

    data class Visibility(
        override val type: String = "visibility_card",
        val visibilityMeters: Int,
        val category: String
    ) : WidgetUi(type)

    data class StormFog(
        override val type: String = "storm_fog_alert_card",
        val alertActive: Boolean,
        val conditionCode: Int,
        val message: String
    ) : WidgetUi(type)

    data class SchoolCommute(
        override val type: String = "school_commute_card",
        val run: String,
        val date: String,
        val window: String,
        val verdict: String,
        val reasons: List<String>,
        val tips: List<String>
    ) : WidgetUi(type)

    data class RainAlert(
        override val type: String = "rain_alert_card",
        val message: String,
        val peakProbabilityPercent: Int,
        val peakHourLabel: String?
    ) : WidgetUi(type)

    data class SevereAlertItem(
        val type: String,
        val startsAt: String,
        val message: String
    )

    data class SevereWeather(
        override val type: String = "severe_weather_card",
        val alerts: List<SevereAlertItem>,
        val message: String
    ) : WidgetUi(type)

    data class WaveHeight(
        override val type: String = "wave_height_card",
        val isCoastal: Boolean = true,
        val waveHeightMeters: Double = 0.0,
        val wavePeriodSeconds: Double = 0.0,
        val swellHeightMeters: Double = 0.0,
        val category: String = "Calm",
        val surfRating: String = "Flat",
        val message: String = ""
    ) : WidgetUi(type)

    data class SeaTemp(
        override val type: String = "sea_temp_card",
        val waterTempCelsius: Double = 24.0,
        val isEstimated: Boolean = false,
        val comfort: String = "Comfortable",
        val message: String = ""
    ) : WidgetUi(type)

    data class Tide(
        override val type: String = "tide_card",
        val date: String = "",
        val nextTideState: String = "High Tide",
        val nextTideTime: String = "",
        val secondaryTideState: String = "Low Tide",
        val secondaryTideTime: String = "",
        val tideSummary: String = "",
        val message: String = ""
    ) : WidgetUi(type)

    data class DestinationItem(
        val id: String,
        val name: String,
        val country: String,
        val lat: Double,
        val lon: Double,
        val tempCelsius: Double,
        val condition: String,
        val weatherCode: Int,
        val packingTip: String
    )

    data class SavedDestinations(
        override val type: String = "saved_destinations_card",
        val destinations: List<DestinationItem> = emptyList(),
        val message: String = ""
    ) : WidgetUi(type)

    data class FlightAlert(
        override val type: String = "flight_alert_card",
        val hasDisruptionRisk: Boolean = false,
        val severity: String = "Normal",
        val flightStatus: String = "On Schedule",
        val reasons: List<String> = emptyList(),
        val message: String = ""
    ) : WidgetUi(type)

    data class PackingSuggestion(
        override val type: String = "packing_suggestion_card",
        val destination: String = "",
        val headline: String = "",
        val items: List<String> = emptyList(),
        val tips: List<String> = emptyList(),
        val tempRange: String = "",
        val rainChancePercent: Int = 0,
        val uvIndexMax: Double = 0.0,
        val message: String = ""
    ) : WidgetUi(type)

    data class StatusOnly(
        override val type: String,
        val status: String,
        val message: String
    ) : WidgetUi(type)

    data class Unsupported(
        override val type: String
    ) : WidgetUi(type)

    // ── Gardener ──────────────────────────────────────────────────────────────
    data class SoilMoisture(
        override val type: String = "soil_moisture_card",
        val level: String = "Normal",
        val precip7dMm: Double = 0.0,
        val humidityPercent: Int = 50,
        val advice: String = "",
        val message: String = ""
    ) : WidgetUi(type)

    data class RainfallPrediction(
        override val type: String = "rainfall_prediction_card",
        val totalMm: Double = 0.0,
        val maxProbabilityPercent: Int = 0,
        val rainyDayCount: Int = 0,
        val forecastDays: Int = 7,
        val outlook: String = "",
        val message: String = ""
    ) : WidgetUi(type)

    data class FrostAlert(
        override val type: String = "frost_alert_card",
        val alertActive: Boolean = false,
        val alertLevel: String = "Clear",
        val frostRiskDayCount: Int = 0,
        val hardFrostDayCount: Int = 0,
        val advice: String = "",
        val message: String = ""
    ) : WidgetUi(type)

    data class PlantingGuidance(
        override val type: String = "planting_guidance_card",
        val hasFrostRisk: Boolean = false,
        val avgMaxTempC: Double = 22.0,
        val avgMinTempC: Double = 12.0,
        val totalRainMm7d: Double = 0.0,
        val guidance: String = "",
        val message: String = ""
    ) : WidgetUi(type)

    // ── Event Planner ─────────────────────────────────────────────────────────
    data class DayForecastItem(
        val date: String,
        val condition: String,
        val maxTempC: Double,
        val minTempC: Double,
        val precipProbPercent: Int
    )

    data class ExtendedForecast(
        override val type: String = "extended_forecast_card",
        val days: List<DayForecastItem> = emptyList(),
        val forecastDayCount: Int = 0,
        val message: String = ""
    ) : WidgetUi(type)

    data class RainProbability(
        override val type: String = "rain_probability_card",
        val peakEventHourProbPercent: Int = 0,
        val avgEventHourProbPercent: Int = 0,
        val daily3MaxProbPercent: Int = 0,
        val outlook: String = "",
        val message: String = ""
    ) : WidgetUi(type)

    data class ComfortIndex(
        override val type: String = "comfort_index_card",
        val comfortIndex: Int = 0,
        val rating: String = "Fair",
        val tempCelsius: Double = 22.0,
        val apparentTempCelsius: Double = 22.0,
        val humidityPercent: Int = 50,
        val uvIndex: Double = 3.0,
        val advice: String = "",
        val message: String = ""
    ) : WidgetUi(type)
}

