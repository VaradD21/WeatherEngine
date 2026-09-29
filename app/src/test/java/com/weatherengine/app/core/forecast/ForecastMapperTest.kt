package com.weatherengine.app.core.forecast

import com.weatherengine.app.data.openmeteo.OpenMeteoAirQualityResponse
import com.weatherengine.app.data.openmeteo.OpenMeteoCurrentWeather
import com.weatherengine.app.data.openmeteo.OpenMeteoForecastResponse
import com.weatherengine.app.data.openmeteo.OpenMeteoHourlyForecast
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadResource(fileName: String): String {
        return javaClass.classLoader?.getResourceAsStream(fileName)?.bufferedReader()?.use { it.readText() }
            ?: error("Resource $fileName not found")
    }

    @Test
    fun testRealFixturesMapping() {
        val forecastJson = loadResource("openmeteo-forecast.json")
        val airQualityJson = loadResource("openmeteo-air-quality.json")

        val forecastDto = json.decodeFromString<OpenMeteoForecastResponse>(forecastJson)
        val airQualityDto = json.decodeFromString<OpenMeteoAirQualityResponse>(airQualityJson)

        val ui = ForecastMapper.mapToForecastUi(
            forecastResponse = forecastDto,
            airQualityResponse = airQualityDto,
            locationLabel = "Mumbai, Maharashtra"
        )

        assertEquals("Mumbai, Maharashtra", ui.locationLabel)
        assertFalse(ui.current.temperatureFormatted.isBlank())
        assertTrue(ui.current.temperatureFormatted.contains("°"))
        assertFalse(ui.hourly.isEmpty())
        assertTrue(ui.days.isNotEmpty())

        // In Mumbai fixture, pollen fields are all null
        assertFalse(ui.pollen.isAvailable)
        assertEquals("Pollen data is not available for this region", ui.pollen.levelLabel)

        // Hourly count should be up to 24
        assertTrue(ui.hourly.size <= 24)
        assertEquals("Weather data by Open-Meteo.com", ui.attribution)
    }

    @Test
    fun testPollenPresentInEuropeanFixture() {
        val forecastJson = loadResource("openmeteo-forecast.json")
        val pollenJson = loadResource("openmeteo-pollen-europe.json")

        val forecastDto = json.decodeFromString<OpenMeteoForecastResponse>(forecastJson)
        val airQualityDto = json.decodeFromString<OpenMeteoAirQualityResponse>(pollenJson)

        val ui = ForecastMapper.mapToForecastUi(
            forecastResponse = forecastDto,
            airQualityResponse = airQualityDto,
            locationLabel = "Berlin, Germany"
        )

        assertTrue(ui.pollen.isAvailable)
        assertEquals("Birch", ui.pollen.highestName)
        assertEquals(45.0, ui.pollen.highestValue!!, 0.01)
        assertTrue(ui.pollen.levelLabel.contains("Moderate"))
    }

    @Test
    fun testOutOfRangeMetricsAreSanitizedToNullOrDash() {
        val dirtyForecast = OpenMeteoForecastResponse(
            latitude = 0.0,
            longitude = 0.0,
            current = OpenMeteoCurrentWeather(
                time = "2026-09-28T12:00",
                temperature2m = 999.0, // Out of range (>60)
                apparentTemperature = -200.0, // Out of range (<-90)
                relativeHumidity2m = 150, // Out of range (>100)
                windSpeed10m = -10.0, // Out of range (<0)
                windGusts10m = -5.0,
                precipitation = -1.0,
                weatherCode = 0,
                isDay = 1
            ),
            hourly = OpenMeteoHourlyForecast(
                time = listOf("2026-09-28T12:00"),
                temperature2m = listOf(150.0), // out of range
                apparentTemperature = listOf(200.0),
                relativeHumidity2m = listOf(-20),
                precipitationProbability = listOf(120),
                weatherCode = listOf(0),
                windSpeed10m = listOf(-1.0),
                windGusts10m = listOf(-1.0),
                visibility = listOf(-50.0),
                uvIndex = listOf(35.0),
                isDay = listOf(1)
            )
        )

        val ui = ForecastMapper.mapToForecastUi(
            forecastResponse = dirtyForecast,
            airQualityResponse = null,
            locationLabel = "Test City"
        )

        assertEquals("—", ui.current.temperatureFormatted)
        assertEquals("", ui.current.apparentTemperatureFormatted)
        assertEquals("—", ui.current.humidityFormatted)
        assertNull(ui.current.humidityPercent)
        assertEquals("—", ui.current.windFormatted)
        assertNull(ui.current.windSpeedKmh)

        // Hourly sanitized
        assertEquals(1, ui.hourly.size)
        val hour = ui.hourly.first()
        assertEquals("—", hour.tempFormatted)
        assertNull(hour.tempRaw)
        assertNull(hour.apparentTempRaw)
        assertNull(hour.precipProb)
        assertNull(hour.windKmh)
        assertNull(hour.visibilityM)
        assertNull(hour.uvIndex)
    }

    @Test
    fun testUnequalHourlyArraysZippedToShortestLength() {
        val unevenForecast = OpenMeteoForecastResponse(
            latitude = 0.0,
            longitude = 0.0,
            current = OpenMeteoCurrentWeather(
                time = "2026-09-28T12:00",
                temperature2m = 25.0
            ),
            hourly = OpenMeteoHourlyForecast(
                time = listOf("2026-09-28T12:00", "2026-09-28T13:00", "2026-09-28T14:00"),
                temperature2m = listOf(25.0, 26.0), // Only 2 items
                apparentTemperature = listOf(25.0, 26.0, 27.0),
                relativeHumidity2m = listOf(50, 50, 50),
                precipitationProbability = listOf(0, 0, 0),
                weatherCode = listOf(0, 0, 0),
                windSpeed10m = listOf(10.0, 10.0, 10.0),
                windGusts10m = listOf(15.0, 15.0, 15.0),
                visibility = listOf(10000.0, 10000.0, 10000.0),
                uvIndex = listOf(2.0, 2.0, 2.0),
                isDay = listOf(1, 1, 1)
            )
        )

        val ui = ForecastMapper.mapToForecastUi(
            forecastResponse = unevenForecast,
            airQualityResponse = null,
            locationLabel = "Test City"
        )

        // Should be trimmed to 2 because temperature2m only has 2 items
        assertEquals(2, ui.hourly.size)
    }

    @Test
    fun testMissingHourlyBlockDoesNotCrash() {
        val noHourlyForecast = OpenMeteoForecastResponse(
            latitude = 0.0,
            longitude = 0.0,
            current = OpenMeteoCurrentWeather(
                time = "2026-09-28T12:00",
                temperature2m = 25.0
            ),
            hourly = null
        )

        val ui = ForecastMapper.mapToForecastUi(
            forecastResponse = noHourlyForecast,
            airQualityResponse = null,
            locationLabel = "Test City"
        )

        assertTrue(ui.hourly.isEmpty())
        assertEquals("25°", ui.current.temperatureFormatted)
    }
}
