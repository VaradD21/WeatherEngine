package com.weatherengine.app.data.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val timezone: String? = null,
    val current: OpenMeteoCurrentWeather? = null,
    val hourly: OpenMeteoHourlyForecast? = null,
    val daily: OpenMeteoDailyForecast? = null
)

@Serializable
data class OpenMeteoCurrentWeather(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature2m: Double? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed10m: Double? = null,
    @SerialName("wind_gusts_10m") val windGusts10m: Double? = null
)

@Serializable
data class OpenMeteoHourlyForecast(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val relativeHumidity2m: List<Int?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m") val windGusts10m: List<Double?> = emptyList(),
    val visibility: List<Double?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList()
)

@Serializable
data class OpenMeteoDailyForecast(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeed10mMax: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList()
)

@Serializable
data class OpenMeteoAirQualityResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val current: OpenMeteoCurrentAirQuality? = null,
    val hourly: OpenMeteoHourlyAirQuality? = null
)

@Serializable
data class OpenMeteoCurrentAirQuality(
    val time: String? = null,
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("grass_pollen") val grassPollen: Double? = null,
    @SerialName("birch_pollen") val birchPollen: Double? = null,
    @SerialName("alder_pollen") val alderPollen: Double? = null,
    @SerialName("ragweed_pollen") val ragweedPollen: Double? = null
)

@Serializable
data class OpenMeteoHourlyAirQuality(
    val time: List<String> = emptyList(),
    @SerialName("us_aqi") val usAqi: List<Int?> = emptyList(),
    @SerialName("pm2_5") val pm25: List<Double?> = emptyList()
)
