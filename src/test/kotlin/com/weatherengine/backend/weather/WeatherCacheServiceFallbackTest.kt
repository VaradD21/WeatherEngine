package com.weatherengine.backend.weather

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class WeatherCacheServiceFallbackTest {

    private val openMeteoClient = mockk<OpenMeteoClient>()

    @Test
    fun `WeatherCacheService when weatherOverrideService is null returns normal bundle without crashing`() {
        val service = WeatherCacheService(
            openMeteoClient = openMeteoClient,
            weatherApiClient = null,
            weatherOverrideService = null,
            cacheManager = null
        )

        every { openMeteoClient.fetchForecast(19.0, 72.0) } returns OpenMeteoForecastResponse(
            latitude = 19.0,
            longitude = 72.0,
            current = OpenMeteoCurrentWeather(temperature2m = 28.5)
        )
        every { openMeteoClient.fetchAirQuality(19.0, 72.0) } returns null
        every { openMeteoClient.fetchMarine(19.0, 72.0) } returns null

        val bundle = service.getCachedWeatherBundle(19.0, 72.0)

        assertNotNull(bundle)
        assertEquals(28.5, bundle.forecast.current?.temperature2m)
    }
}
