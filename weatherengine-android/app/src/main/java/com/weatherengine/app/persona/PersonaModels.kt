package com.weatherengine.app.persona

import kotlinx.serialization.Serializable

@Serializable
data class HourlyRowUi(
    val timeIso: String,
    val timeLabel: String,
    val tempFormatted: String,
    val tempRaw: Double? = null,
    val apparentTempRaw: Double? = null,
    val condition: String,
    val iconKey: String,
    val isDay: Boolean = true,
    val precipProb: Int? = null,
    val windKmh: Double? = null,
    val visibilityM: Double? = null,
    val uvIndex: Double? = null,
    val aqi: Int? = null,
    val weatherCode: Int? = null
)

@Serializable
sealed interface DaylightState {
    @Serializable
    data class BeforeSunrise(
        val timeUntilSunriseFormatted: String,
        val dayLengthFormatted: String,
        val sunriseFormatted: String,
        val sunsetFormatted: String
    ) : DaylightState

    @Serializable
    data class Daylight(
        val timeUntilSunsetFormatted: String,
        val dayLengthFormatted: String,
        val sunriseFormatted: String,
        val sunsetFormatted: String
    ) : DaylightState

    @Serializable
    data class AfterSunset(
        val dayLengthFormatted: String,
        val sunriseFormatted: String,
        val sunsetFormatted: String
    ) : DaylightState

    @Serializable
    data object Unavailable : DaylightState
}

@Serializable
sealed interface BestRunningResult {
    @Serializable
    data class Optimal(
        val startHourLabel: String,
        val endHourLabel: String,
        val score: Int,
        val reasons: List<String>
    ) : BestRunningResult

    @Serializable
    data class NoGoodWindow(val reason: String) : BestRunningResult
}

@Serializable
sealed interface HeatStatus {
    @Serializable
    data class Alert(val message: String, val note: String) : HeatStatus

    @Serializable
    data class Caution(val message: String, val note: String) : HeatStatus

    @Serializable
    data object None : HeatStatus
}

@Serializable
data class StormFogStatus(
    val alertActive: Boolean,
    val message: String
)

@Serializable
data class CommuteRainResult(
    val peakRainChance: Int,
    val peakHourLabel: String,
    val advice: String
)

@Serializable
data class PollenInfo(
    val highestValue: Double? = null,
    val highestName: String? = null,
    val levelLabel: String = "Unavailable",
    val isAvailable: Boolean = false
)

enum class PersonaWidgetType {
    AQI,
    UV,
    HUMIDITY,
    POLLEN,
    SUN,
    BEST_RUNNING,
    WIND,
    HEAT,
    VISIBILITY,
    STORM_FOG,
    COMMUTE_RAIN,
    TRAFFIC
}

// App heuristics, not official warnings
object PersonaConstants {

    const val PERSONA_PARENT_CODE = "parent"
    const val PERSONA_PARENT_NAME = "Parent & Family"
    const val PERSONA_PARENT_DESC = "School commute alerts, rain lookahead, and severe weather warnings for families."

    // Sane value ranges for raw provider data sanitization
    const val MIN_TEMP_C = -100.0
    const val MAX_TEMP_C = 70.0
    const val MIN_HUMIDITY = 0
    const val MAX_HUMIDITY = 100
    const val MIN_VISIBILITY_M = 0.0
    const val MIN_UV = 0.0
    const val MAX_UV = 20.0

    // UV Index Thresholds
    const val UV_LOW_MAX = 2.0
    const val UV_MODERATE_MAX = 5.0
    const val UV_HIGH_MAX = 7.0
    const val UV_VERY_HIGH_MAX = 10.0

    // Humidity Comfort Thresholds (%)
    const val HUMIDITY_DRY_MAX = 29
    const val HUMIDITY_COMFORTABLE_MAX = 60
    const val HUMIDITY_HUMID_MAX = 80

    // US AQI Thresholds
    const val AQI_GOOD_MAX = 50
    const val AQI_MODERATE_MAX = 100
    const val AQI_SENSITIVE_MAX = 150
    const val AQI_UNHEALTHY_MAX = 200
    const val AQI_VERY_UNHEALTHY_MAX = 300

    // Visibility Thresholds (metres)
    const val VISIBILITY_VERY_POOR_MAX = 999.0
    const val VISIBILITY_POOR_MAX = 3999.0
    const val VISIBILITY_MODERATE_MAX = 9999.0

    // Wind Speed Thresholds (km/h)
    const val WIND_LIGHT_MAX = 11.9
    const val WIND_BREEZY_MAX = 28.9
    const val WIND_STRONG_MAX = 49.9

    // Best Running Window Scoring
    const val RUN_CANDIDATE_START_HOUR = 4
    const val RUN_CANDIDATE_END_HOUR = 22
    const val RUN_MIN_ACCEPTABLE_SCORE = 40
    const val RUN_WINDOW_CONSECUTIVE_HOURS = 2

    // Apparent Temperature Penalties (Running)
    const val RUN_PENALTY_TEMP_FREEZING = 30
    const val RUN_PENALTY_TEMP_CHILLY = 10
    const val RUN_PENALTY_TEMP_WARM = 10
    const val RUN_PENALTY_TEMP_HOT = 30
    const val RUN_PENALTY_TEMP_VERY_HOT = 50
    const val RUN_PENALTY_TEMP_EXTREME = 80

    // Rain Penalties (Running)
    const val RUN_PENALTY_RAIN_HEAVY = 50
    const val RUN_PENALTY_RAIN_MODERATE = 25
    const val RUN_PENALTY_RAIN_LIGHT = 5

    // Wind Penalties (Running)
    const val RUN_PENALTY_WIND_GALE = 40
    const val RUN_PENALTY_WIND_STRONG = 20
    const val RUN_PENALTY_WIND_MODERATE = 5

    // Air Quality Penalties (Running)
    const val RUN_PENALTY_AQI_HAZARDOUS = 60
    const val RUN_PENALTY_AQI_UNHEALTHY = 35
    const val RUN_PENALTY_AQI_MODERATE = 10

    // UV Penalties (Running)
    const val RUN_PENALTY_UV_VERY_HIGH = 20
    const val RUN_PENALTY_UV_HIGH = 10

    // Other Penalties (Running)
    const val RUN_PENALTY_LOW_VISIBILITY = 30

    // Heat Alert Thresholds (°C feels-like)
    const val HEAT_CAUTION_MIN = 32.0
    const val HEAT_ALERT_MIN = 36.0
    const val HEAT_DISCLAIMER_NOTE = "Based on feels-like temperature, not an official IMD warning"

    // Storm and Fog lookahead windows
    const val STORM_LOOKAHEAD_HOURS = 12
    const val FOG_LOOKAHEAD_HOURS = 6
    const val COMMUTE_LOOKAHEAD_HOURS = 6

    // Commute Rain Probability Thresholds (%)
    const val COMMUTE_RAIN_HIGH_THRESHOLD = 50
    const val COMMUTE_RAIN_MODERATE_THRESHOLD = 20
}
