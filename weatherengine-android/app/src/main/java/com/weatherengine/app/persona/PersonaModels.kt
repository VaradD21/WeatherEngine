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
    val precipProbFormatted: String = "",
    val windKmh: Double? = null,
    val windGustsKmh: Double? = null,
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
    data class Alert(val active: Boolean, val message: String, val note: String) : HeatStatus

    @Serializable
    data class Caution(val message: String, val note: String) : HeatStatus

    @Serializable
    data object None : HeatStatus
}

@Serializable
data class StormFogStatus(
    val alertActive: Boolean,
    val stormHour: String? = null,
    val fogHour: String? = null,
    val message: String
)

@Serializable
data class CommuteRainResult(
    val peakRainChance: Int,
    val peakHourLabel: String,
    val advice: String,
    val minVisibilityM: Double? = null
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
