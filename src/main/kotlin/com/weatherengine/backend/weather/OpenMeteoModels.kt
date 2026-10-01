package com.weatherengine.backend.weather

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenMeteoForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @JsonProperty("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val timezone: String? = null,
    val current: OpenMeteoCurrentWeather? = null,
    val hourly: OpenMeteoHourlyForecast? = null,
    val daily: OpenMeteoDailyForecast? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenMeteoCurrentWeather(
    val time: String? = null,
    @JsonProperty("temperature_2m") val temperature2m: Double? = null,
    @JsonProperty("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @JsonProperty("apparent_temperature") val apparentTemperature: Double? = null,
    @JsonProperty("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @JsonProperty("weather_code") val weatherCode: Int? = null,
    @JsonProperty("wind_speed_10m") val windSpeed10m: Double? = null,
    @JsonProperty("wind_gusts_10m") val windGusts10m: Double? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenMeteoHourlyForecast(
    val time: List<String> = emptyList(),
    @JsonProperty("temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @JsonProperty("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @JsonProperty("relative_humidity_2m") val relativeHumidity2m: List<Int?> = emptyList(),
    @JsonProperty("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @JsonProperty("weather_code") val weatherCode: List<Int?> = emptyList(),
    @JsonProperty("wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @JsonProperty("wind_gusts_10m") val windGusts10m: List<Double?> = emptyList(),
    val visibility: List<Double?> = emptyList(),
    @JsonProperty("uv_index") val uvIndex: List<Double?> = emptyList(),
    @JsonProperty("is_day") val isDay: List<Int?> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenMeteoDailyForecast(
    val time: List<String> = emptyList(),
    @JsonProperty("weather_code") val weatherCode: List<Int> = emptyList(),
    @JsonProperty("temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @JsonProperty("temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @JsonProperty("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @JsonProperty("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @JsonProperty("wind_speed_10m_max") val windSpeed10mMax: List<Double?> = emptyList(),
    @JsonProperty("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenMeteoAirQualityResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val current: OpenMeteoCurrentAirQuality? = null,
    val hourly: OpenMeteoHourlyAirQuality? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenMeteoCurrentAirQuality(
    val time: String? = null,
    @JsonProperty("us_aqi") val usAqi: Int? = null,
    @JsonProperty("pm2_5") val pm25: Double? = null,
    @JsonProperty("grass_pollen") val grassPollen: Double? = null,
    @JsonProperty("birch_pollen") val birchPollen: Double? = null,
    @JsonProperty("alder_pollen") val alderPollen: Double? = null,
    @JsonProperty("ragweed_pollen") val ragweedPollen: Double? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenMeteoHourlyAirQuality(
    val time: List<String> = emptyList(),
    @JsonProperty("us_aqi") val usAqi: List<Int?> = emptyList(),
    @JsonProperty("pm2_5") val pm25: List<Double?> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherBundleDto(
    val forecast: OpenMeteoForecastResponse = OpenMeteoForecastResponse(),
    val airQuality: OpenMeteoAirQualityResponse? = null,
    val fetchedAtMs: Long = System.currentTimeMillis()
)

data class WidgetDto(
    val type: String,
    val data: Map<String, Any?>
)

data class HomepageResponse(
    val widgets: List<WidgetDto> = emptyList(),
    val message: String? = null
)
