package com.weatherengine.backend.weather

import com.weatherengine.backend.config.WEATHER_CACHE_NAME
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(WeatherCachingProxyTest.TestCacheConfig::class)
class WeatherCachingProxyTest {

    @Configuration
    @EnableCaching
    class TestCacheConfig {
        @Bean
        fun cacheManager(): CacheManager = ConcurrentMapCacheManager(WEATHER_CACHE_NAME)

        @Bean
        fun openMeteoClient(): OpenMeteoClient = mockk()

        @Bean
        fun weatherApiClient(): WeatherApiClient = mockk(relaxed = true)

        @Bean
        fun weatherCacheService(
            openMeteoClient: OpenMeteoClient,
            weatherApiClient: WeatherApiClient
        ): WeatherCacheService = WeatherCacheService(openMeteoClient, weatherApiClient)
    }

    @Autowired
    private lateinit var weatherCacheService: WeatherCacheService

    @Autowired
    private lateinit var openMeteoClient: OpenMeteoClient

    @Test
    fun `Spring Cacheable proxy intercepts calls and hits Open-Meteo client only once on repeated requests`() {
        val sample = OpenMeteoForecastResponse(latitude = 19.07, longitude = 72.87)
        every { openMeteoClient.fetchForecast(19.07, 72.87) } returns sample
        every { openMeteoClient.fetchAirQuality(19.07, 72.87) } returns null
        every { openMeteoClient.fetchMarine(19.07, 72.87) } returns null

        // First call - cache miss, should hit openMeteoClient
        val bundle1 = weatherCacheService.getCachedWeatherBundle(19.07, 72.87)
        assertEquals(19.07, bundle1.forecast.latitude)

        // Second call with same coordinates - should hit cache, NOT openMeteoClient
        val bundle2 = weatherCacheService.getCachedWeatherBundle(19.07, 72.87)
        assertEquals(19.07, bundle2.forecast.latitude)

        // Verify openMeteoClient was invoked exactly once
        verify(exactly = 1) { openMeteoClient.fetchForecast(19.07, 72.87) }
    }
}
