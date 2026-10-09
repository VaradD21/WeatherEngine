package com.weatherengine.backend.weather

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.weatherengine.backend.common.ApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

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
    val precipitation: List<Double?> = emptyList(),
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
    val current: OpenMeteoCurrentAirQuality? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenMeteoCurrentAirQuality(
    @JsonProperty("us_aqi") val usAqi: Int? = null,
    @JsonProperty("pm2_5") val pm25: Double? = null,
    @JsonProperty("grass_pollen") val grassPollen: Double? = null,
    @JsonProperty("birch_pollen") val birchPollen: Double? = null,
    @JsonProperty("alder_pollen") val alderPollen: Double? = null,
    @JsonProperty("ragweed_pollen") val ragweedPollen: Double? = null
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

@Component
class OpenMeteoClient(
    private val restClient: RestClient,
    @Value("\${app.weather-providers.open-meteo.forecast-url:https://api.open-meteo.com/v1/forecast}")
    private val forecastBaseUrl: String,
    @Value("\${app.weather-providers.open-meteo.air-quality-url:https://air-quality-api.open-meteo.com/v1/air-quality}")
    private val airQualityBaseUrl: String
) {
    private val logger = LoggerFactory.getLogger(OpenMeteoClient::class.java)

    companion object {
        private const val CURRENT_PARAMS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_gusts_10m"
        private const val HOURLY_PARAMS =
            "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_gusts_10m,visibility,uv_index,is_day"
        private const val DAILY_PARAMS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,uv_index_max,sunrise,sunset"
        private const val AQI_CURRENT_PARAMS =
            "us_aqi,pm2_5,grass_pollen,birch_pollen,alder_pollen,ragweed_pollen"
    }

    fun fetchForecast(lat: Double, lon: Double): OpenMeteoForecastResponse {
        val uri = UriComponentsBuilder.fromHttpUrl(forecastBaseUrl)
            .queryParam("latitude", lat)
            .queryParam("longitude", lon)
            .queryParam("current", CURRENT_PARAMS)
            .queryParam("hourly", HOURLY_PARAMS)
            .queryParam("daily", DAILY_PARAMS)
            .queryParam("timezone", "auto")
            .queryParam("forecast_days", 7)
            .build()
            .toUri()

        val response = try {
            restClient.get()
                .uri(uri)
                .retrieve()
                .body(OpenMeteoForecastResponse::class.java)
        } catch (ex: Exception) {
            logger.error("Failed to fetch forecast from Open-Meteo for ({}, {})", lat, lon, ex)
            throw ApiException(HttpStatus.BAD_GATEWAY, "Upstream weather provider is temporarily unavailable")
        }

        return response ?: throw ApiException(HttpStatus.BAD_GATEWAY, "Empty response from upstream weather provider")
    }

    fun fetchAirQuality(lat: Double, lon: Double): OpenMeteoAirQualityResponse? {
        val uri = UriComponentsBuilder.fromHttpUrl(airQualityBaseUrl)
            .queryParam("latitude", lat)
            .queryParam("longitude", lon)
            .queryParam("current", AQI_CURRENT_PARAMS)
            .queryParam("timezone", "auto")
            .queryParam("forecast_days", 2)
            .build()
            .toUri()

        return try {
            restClient.get()
                .uri(uri)
                .retrieve()
                .body(OpenMeteoAirQualityResponse::class.java)
        } catch (ex: Exception) {
            logger.warn("Air quality provider call failed for ({}, {}): {}", lat, lon, ex.message)
            null
        }
    }
}

