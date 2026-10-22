package com.weatherengine.backend.weather

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.PersonaService
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.*

class WeatherApiValidationTest {

    private val weatherCacheService = mockk<WeatherCacheService>()
    private val personaService = mockk<PersonaService>()
    private val widgetBuilder = WidgetBuilder()
    private val controller = WeatherController(weatherCacheService, personaService, widgetBuilder)

    private val testUser = AuthenticatedUser(UUID.randomUUID(), "qa@example.com")

    // 5.10: Invalid lat/lon (999, NaN, Infinity)
    @Test
    fun `5_10 - Invalid latitude above 90 throws 400 BAD_REQUEST`() {
        val ex = assertThrows(ApiException::class.java) {
            controller.getWeather(999.0, 77.0)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
        assertTrue(ex.message.contains("Latitude must be between -90 and 90"))
    }

    @Test
    fun `5_10 - Invalid latitude below -90 throws 400 BAD_REQUEST`() {
        val ex = assertThrows(ApiException::class.java) {
            controller.getWeather(-91.0, 77.0)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
        assertTrue(ex.message.contains("Latitude must be between -90 and 90"))
    }

    @Test
    fun `5_10 - Invalid longitude above 180 throws 400 BAD_REQUEST`() {
        val ex = assertThrows(ApiException::class.java) {
            controller.getWeather(28.0, 185.0)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
        assertTrue(ex.message.contains("Longitude must be between -180 and 180"))
    }

    @Test
    fun `5_10 - NaN coordinates throw 400 BAD_REQUEST`() {
        val ex1 = assertThrows(ApiException::class.java) {
            controller.getWeather(Double.NaN, 77.0)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex1.status)

        val ex2 = assertThrows(ApiException::class.java) {
            controller.getWeather(28.0, Double.NaN)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex2.status)
    }

    @Test
    fun `5_10 - Infinite coordinates throw 400 BAD_REQUEST`() {
        val ex = assertThrows(ApiException::class.java) {
            controller.getWeather(Double.POSITIVE_INFINITY, 77.0)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    // 5.14: Test WMO codes mapping
    @Test
    fun `5_14 - EventPlanner and Storm cards map WMO codes safely without crashing`() {
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        val daily = OpenMeteoDailyForecast(
            time = codes.indices.map { "2026-10-${(it + 1).toString().padStart(2, '0')}" },
            weatherCode = codes,
            temperature2mMax = List(codes.size) { 25.0 },
            temperature2mMin = List(codes.size) { 15.0 },
            precipitationProbabilityMax = List(codes.size) { 20 }
        )

        val res = EventPlannerRules.computeExtendedForecastCard(daily)
        assertEquals(codes.size.coerceAtMost(EventPlannerConstants.FORECAST_DAYS), res["forecastDayCount"])
        @Suppress("UNCHECKED_CAST")
        val days = res["days"] as List<Map<String, Any?>>
        days.forEach { day ->
            assertNotNull(day["condition"])
            assertFalse((day["condition"] as String).isBlank())
        }
    }
}
