package com.weatherengine.backend.weather

import com.weatherengine.backend.common.ApiException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class WeatherCacheServiceTest {

    private val openMeteoClient = mockk<OpenMeteoClient>()
    private val weatherApiClient = mockk<WeatherApiClient>(relaxed = true)
    private val weatherCacheService = WeatherCacheService(openMeteoClient, weatherApiClient)

    private val sampleForecast = OpenMeteoForecastResponse(
        latitude = 19.07,
        longitude = 72.87,
        current = OpenMeteoCurrentWeather(temperature2m = 28.5)
    )

    private val sampleAirQuality = OpenMeteoAirQualityResponse(
        current = OpenMeteoCurrentAirQuality(usAqi = 45, pm25 = 12.0)
    )

    private val sampleMarine = OpenMeteoMarineResponse(
        hourly = OpenMeteoMarineHourly(waveHeight = listOf(1.2, 1.4))
    )

    @Test
    fun `getCachedWeatherBundle returns all providers when all succeed`() {
        every { openMeteoClient.fetchForecast(19.07, 72.87) } returns sampleForecast
        every { openMeteoClient.fetchAirQuality(19.07, 72.87) } returns sampleAirQuality
        every { openMeteoClient.fetchMarine(19.07, 72.87) } returns sampleMarine
        every { weatherApiClient.isConfigured() } returns false

        val bundle = weatherCacheService.getCachedWeatherBundle(19.07, 72.87)

        assertNotNull(bundle)
        assertEquals(sampleForecast, bundle.forecast)
        assertEquals(sampleAirQuality, bundle.airQuality)
        assertEquals(sampleMarine, bundle.marine)
    }

    @Test
    fun `air quality failure degrades gracefully to null without failing entire bundle`() {
        every { openMeteoClient.fetchForecast(19.07, 72.87) } returns sampleForecast
        every { openMeteoClient.fetchAirQuality(19.07, 72.87) } throws RuntimeException("Air quality API timeout")
        every { openMeteoClient.fetchMarine(19.07, 72.87) } returns sampleMarine
        every { weatherApiClient.isConfigured() } returns false

        val bundle = weatherCacheService.getCachedWeatherBundle(19.07, 72.87)

        assertNotNull(bundle)
        assertEquals(sampleForecast, bundle.forecast)
        assertNull(bundle.airQuality, "Air quality should degrade to null on failure")
        assertEquals(sampleMarine, bundle.marine)
    }

    @Test
    fun `marine failure degrades gracefully to null without failing entire bundle`() {
        every { openMeteoClient.fetchForecast(19.07, 72.87) } returns sampleForecast
        every { openMeteoClient.fetchAirQuality(19.07, 72.87) } returns sampleAirQuality
        every { openMeteoClient.fetchMarine(19.07, 72.87) } throws RuntimeException("Inland location has no marine data")
        every { weatherApiClient.isConfigured() } returns false

        val bundle = weatherCacheService.getCachedWeatherBundle(19.07, 72.87)

        assertNotNull(bundle)
        assertEquals(sampleForecast, bundle.forecast)
        assertEquals(sampleAirQuality, bundle.airQuality)
        assertNull(bundle.marine, "Marine data should degrade to null on failure")
    }

    @Test
    fun `both air quality and marine failing still returns bundle with forecast`() {
        every { openMeteoClient.fetchForecast(19.07, 72.87) } returns sampleForecast
        every { openMeteoClient.fetchAirQuality(19.07, 72.87) } throws RuntimeException("AQ down")
        every { openMeteoClient.fetchMarine(19.07, 72.87) } throws RuntimeException("Marine down")
        every { weatherApiClient.isConfigured() } returns false

        val bundle = weatherCacheService.getCachedWeatherBundle(19.07, 72.87)

        assertNotNull(bundle)
        assertEquals(sampleForecast, bundle.forecast)
        assertNull(bundle.airQuality)
        assertNull(bundle.marine)
    }

    @Test
    fun `forecast failure propagates and fails the request`() {
        every { openMeteoClient.fetchForecast(19.07, 72.87) } throws ApiException(HttpStatus.BAD_GATEWAY, "Open-Meteo unavailable")
        every { openMeteoClient.fetchAirQuality(19.07, 72.87) } returns sampleAirQuality
        every { openMeteoClient.fetchMarine(19.07, 72.87) } returns sampleMarine

        val ex = assertThrows<ApiException> {
            weatherCacheService.getCachedWeatherBundle(19.07, 72.87)
        }
        assertEquals(HttpStatus.BAD_GATEWAY, ex.status)
    }
}
