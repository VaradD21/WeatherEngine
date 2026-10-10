package com.weatherengine.backend.weather

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.util.Locale

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiAirQuality(
    @JsonProperty("us-epa-index") val usEpaIndex: Int? = null,
    val pm2_5: Double? = null,
    val pm10: Double? = null,
    val o3: Double? = null,
    val no2: Double? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiCurrent(
    @JsonProperty("temp_c") val tempC: Double? = null,
    val humidity: Int? = null,
    @JsonProperty("feelslike_c") val feelslikeC: Double? = null,
    val uv: Double? = null,
    @JsonProperty("air_quality") val airQuality: WeatherApiAirQuality? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiResponse(
    val current: WeatherApiCurrent? = null
)

@Component
class WeatherApiClient(
    private val restClient: RestClient,
    @Value("\${app.weather-providers.weather-api.key:}") private val apiKey: String,
    @Value("\${app.weather-providers.weather-api.url:https://api.weatherapi.com/v1/forecast.json}") private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(WeatherApiClient::class.java)

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    fun fetchCurrentAndAqi(lat: Double, lon: Double): WeatherApiResponse? {
        if (!isConfigured()) return null
        return try {
            val uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("key", apiKey)
                .queryParam("q", String.format(Locale.US, "%.4f,%.4f", lat, lon))
                .queryParam("days", 1)
                .queryParam("aqi", "yes")
                .build(true)
                .toUri()

            restClient.get()
                .uri(uri)
                .retrieve()
                .body(WeatherApiResponse::class.java)
        } catch (ex: Exception) {
            logger.warn("WeatherAPI request failed for ({}, {}): {}", lat, lon, ex.message)
            null
        }
    }
}
