package com.weatherengine.backend.weather

import com.weatherengine.backend.common.ApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

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
            "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m,wind_gusts_10m,visibility,uv_index,is_day"
        private const val DAILY_PARAMS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,uv_index_max,sunrise,sunset"
        private const val AQI_CURRENT_PARAMS =
            "us_aqi,pm2_5,grass_pollen,birch_pollen,alder_pollen,ragweed_pollen"
        private const val AQI_HOURLY_PARAMS = "us_aqi,pm2_5"
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

        return try {
            restClient.get()
                .uri(uri)
                .retrieve()
                .body(OpenMeteoForecastResponse::class.java)
                ?: throw ApiException(HttpStatus.BAD_GATEWAY, "Empty response from upstream weather provider")
        } catch (ex: ApiException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to fetch forecast from Open-Meteo for ({}, {})", lat, lon, ex)
            throw ApiException(HttpStatus.BAD_GATEWAY, "Upstream weather provider is temporarily unavailable")
        }
    }

    fun fetchAirQuality(lat: Double, lon: Double): OpenMeteoAirQualityResponse? {
        val uri = UriComponentsBuilder.fromHttpUrl(airQualityBaseUrl)
            .queryParam("latitude", lat)
            .queryParam("longitude", lon)
            .queryParam("current", AQI_CURRENT_PARAMS)
            .queryParam("hourly", AQI_HOURLY_PARAMS)
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
