package com.weatherengine.app.core.forecast

import com.weatherengine.app.persona.BestRunningResult
import com.weatherengine.app.persona.CommuteRainResult
import com.weatherengine.app.persona.DaylightState
import com.weatherengine.app.persona.HeatStatus
import com.weatherengine.app.persona.HourlyRowUi
import com.weatherengine.app.persona.PollenInfo
import com.weatherengine.app.persona.StormFogStatus
import kotlinx.serialization.Serializable

@Serializable
data class ForecastUi(
    val locationLabel: String,
    val updatedAtLabel: String,
    val timestampMs: Long,
    val current: CurrentWeatherUi,
    val hourly: List<HourlyRowUi> = emptyList(),
    val days: List<DayForecastUi> = emptyList(),
    val daylight: DaylightState = DaylightState.Unavailable,
    val bestRunning: BestRunningResult = BestRunningResult.NoGoodWindow("Unavailable"),
    val heat: HeatStatus = HeatStatus.None,
    val stormFog: StormFogStatus = StormFogStatus(false, message = "Clear conditions"),
    val commute: CommuteRainResult = CommuteRainResult(0, "—", "Dry commute expected", null),
    val pollen: PollenInfo = PollenInfo(),
    val uvMaxToday: Double? = null,
    val attribution: String = "Weather data by Open-Meteo.com",
    val attributionUrl: String = "https://open-meteo.com/",
    val isCached: Boolean = false
)

@Serializable
data class CurrentWeatherUi(
    val temperatureFormatted: String,
    val apparentTemperatureFormatted: String,
    val condition: String,
    val iconKey: String,
    val humidityFormatted: String,
    val humidityPercent: Int? = null,
    val windFormatted: String,
    val windSpeedKmh: Double? = null,
    val windGustsFormatted: String = "",
    val windGustsKmh: Double? = null,
    val precipitationFormatted: String,
    val visibilityMeters: Double? = null,
    val uvIndex: Double? = null,
    val aqi: Int? = null,
    val aqiCategory: String? = null,
    val pm25: Double? = null,
    val isDay: Boolean = true
)

@Serializable
data class DayForecastUi(
    val dateIso: String,
    val dayLabel: String,
    val condition: String,
    val iconKey: String,
    val maxTempFormatted: String,
    val minTempFormatted: String,
    val maxTempRaw: Double,
    val minTempRaw: Double,
    val precipitationProbability: Int? = null,
    val precipitationProbabilityFormatted: String = "",
    val precipitationSumMm: Double? = null,
    val windSpeedMaxKmh: Double? = null,
    val uvIndexMax: Double? = null,
    val sunriseFormatted: String = "—",
    val sunsetFormatted: String = "—",
    val rangeFractionStart: Float = 0f,
    val rangeFractionEnd: Float = 1f
)
