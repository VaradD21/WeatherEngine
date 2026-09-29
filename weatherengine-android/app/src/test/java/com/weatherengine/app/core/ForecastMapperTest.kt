package com.weatherengine.app.core

import com.weatherengine.app.core.forecast.ForecastMapper
import com.weatherengine.app.core.forecast.WmoWeatherCode
import com.weatherengine.app.data.openmeteo.OpenMeteoAirQualityResponse
import com.weatherengine.app.data.openmeteo.OpenMeteoCurrentAirQuality
import com.weatherengine.app.data.openmeteo.OpenMeteoCurrentWeather
import com.weatherengine.app.data.openmeteo.OpenMeteoForecastResponse
import com.weatherengine.app.data.openmeteo.OpenMeteoHourlyForecast
import com.weatherengine.app.persona.PersonaRules
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ForecastMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testWmoWeatherCodeMapping() {
        val clearDay = WmoWeatherCode.info(0, isDay = true)
        assertEquals("Sunny", clearDay.description)
        assertEquals("clear_day", clearDay.iconKey)

        val clearNight = WmoWeatherCode.info(0, isDay = false)
        assertEquals("Clear", clearNight.description)
        assertEquals("clear_night", clearNight.iconKey)

        val rain = WmoWeatherCode.info(61, isDay = true)
        assertEquals("Slight rain", rain.description)
        assertEquals("rain", rain.iconKey)

        val storm = WmoWeatherCode.info(95, isDay = true)
        assertEquals("Thunderstorm", storm.description)
        assertEquals("thunderstorm", storm.iconKey)

        val unknown = WmoWeatherCode.info(null)
        assertEquals("Unknown", unknown.description)
    }

    @Test
    fun testFormatTemp() {
        assertEquals("31°", ForecastMapper.formatTemp(31.2))
        assertEquals("32°", ForecastMapper.formatTemp(31.8))
        assertEquals("-5°", ForecastMapper.formatTemp(-4.6))
        assertEquals("0°", ForecastMapper.formatTemp(0.0))
        assertEquals("—", ForecastMapper.formatTemp(null))
        assertEquals("—", ForecastMapper.formatTemp(Double.NaN))
        assertEquals("—", ForecastMapper.formatTemp(150.0)) // out of sane range
        assertEquals("—", ForecastMapper.formatTemp(-150.0))
    }

    @Test
    fun testDayLabel() {
        val today = LocalDate.of(2026, 9, 28)
        assertEquals("Today", ForecastMapper.dayLabel("2026-09-28", 0, today))
        assertEquals("Tomorrow", ForecastMapper.dayLabel("2026-09-29", 1, today))
        val day3 = ForecastMapper.dayLabel("2026-09-30", 2, today)
        assertTrue(day3.isNotBlank() && day3 != "Today" && day3 != "Tomorrow")
    }

    @Test
    fun testFormatUpdatedAt() {
        val now = 1000000000000L
        assertEquals("Updated just now", ForecastMapper.formatUpdatedAt(now - 30_000L, now))
        assertEquals("Updated 5 min ago", ForecastMapper.formatUpdatedAt(now - 5 * 60_000L, now))
        assertEquals("Updated 2h ago", ForecastMapper.formatUpdatedAt(now - 2 * 3600_000L, now))
    }

    @Test
    fun testHourly_WithNullsAndOutOfRange() {
        val hourly = OpenMeteoHourlyForecast(
            time = listOf("2026-09-28T10:00", "2026-09-28T11:00"),
            temperature2m = listOf(25.0, 150.0), // 150 is out of range -> becomes null
            apparentTemperature = listOf(26.0, null),
            relativeHumidity2m = listOf(60, 120), // 120 is out of range -> becomes null
            precipitationProbability = listOf(null, 150), // 150 -> becomes null
            weatherCode = listOf(0, 1),
            windSpeed10m = listOf(10.0, -5.0), // negative -> null
            windGusts10m = listOf(15.0, null),
            visibility = listOf(-10.0, 10000.0), // negative -> null
            uvIndex = listOf(5.0, 25.0), // 25 -> null
            isDay = listOf(1, 1)
        )
        val response = OpenMeteoForecastResponse(
            current = OpenMeteoCurrentWeather(time = "2026-09-28T10:00", temperature2m = 25.0),
            hourly = hourly
        )

        val ui = ForecastMapper.mapToForecastUi(response, null, "Test")
        assertEquals(2, ui.hourly.size)

        val row0 = ui.hourly[0]
        assertEquals(25.0, row0.tempRaw)
        assertNull(row0.visibilityM)

        val row1 = ui.hourly[1]
        assertNull(row1.tempRaw)
        assertEquals("—", row1.tempFormatted)
        assertNull(row1.apparentTempRaw)
        assertNull(row1.uvIndex)
    }

    @Test
    fun testHourly_UnequalArrayLengths_ZipsUsingShortestLength() {
        val hourly = OpenMeteoHourlyForecast(
            time = listOf("2026-09-28T10:00", "2026-09-28T11:00", "2026-09-28T12:00"),
            temperature2m = listOf(25.0, 26.0), // Length 2
            apparentTemperature = listOf(25.0, 26.0, 27.0),
            relativeHumidity2m = listOf(60, 60, 60),
            precipitationProbability = listOf(0, 0, 0),
            weatherCode = listOf(0, 0, 0),
            windSpeed10m = listOf(10.0, 10.0, 10.0),
            windGusts10m = listOf(15.0, 15.0, 15.0),
            visibility = listOf(10000.0, 10000.0, 10000.0),
            uvIndex = listOf(1.0, 2.0, 3.0),
            isDay = listOf(1, 1, 1)
        )
        val response = OpenMeteoForecastResponse(
            current = OpenMeteoCurrentWeather(time = "2026-09-28T10:00", temperature2m = 25.0),
            hourly = hourly
        )

        val ui = ForecastMapper.mapToForecastUi(response, null, "Test")
        // Must take minimum size 2 without crashing with IndexOutOfBounds
        assertEquals(2, ui.hourly.size)
    }

    @Test
    fun testHourly_MissingOrEmptyHourlyBlock_StillParsesCleanly() {
        val response = OpenMeteoForecastResponse(
            current = OpenMeteoCurrentWeather(time = "2026-09-28T10:00", temperature2m = 25.0),
            hourly = null // Missing
        )

        val ui = ForecastMapper.mapToForecastUi(response, null, "Test")
        assertTrue(ui.hourly.isEmpty())
        assertEquals("25°", ui.current.temperatureFormatted)
    }

    @Test
    fun testPollen_AbsentOrNull_ShowsUnavailable() {
        val aqiNullPollen = OpenMeteoAirQualityResponse(
            current = OpenMeteoCurrentAirQuality(
                time = "2026-09-28T10:00",
                usAqi = 50,
                grassPollen = null,
                birchPollen = null,
                alderPollen = null,
                ragweedPollen = null
            )
        )
        val response = OpenMeteoForecastResponse(current = OpenMeteoCurrentWeather(time = "2026-09-28T10:00"))
        val ui = ForecastMapper.mapToForecastUi(response, aqiNullPollen, "Mumbai")

        assertFalse(ui.pollen.isAvailable)
        assertNull(ui.pollen.highestValue)
        assertEquals("Pollen data is not available for this region", ui.pollen.levelLabel)
    }

    @Test
    fun testPollen_Present_MapsHighestAccurately() {
        val europePollenStream = javaClass.classLoader?.getResourceAsStream("openmeteo-pollen-europe.json")
        assertNotNull(europePollenStream)
        val aqiDto = json.decodeFromString<OpenMeteoAirQualityResponse>(europePollenStream!!.bufferedReader().readText())
        val response = OpenMeteoForecastResponse(current = OpenMeteoCurrentWeather(time = "2026-09-28T08:00"))

        val ui = ForecastMapper.mapToForecastUi(response, aqiDto, "Berlin")
        assertTrue(ui.pollen.isAvailable)
        assertEquals(45.0, ui.pollen.highestValue!!, 0.01)
        assertEquals("Birch", ui.pollen.highestName)
        assertTrue(ui.pollen.levelLabel.contains("Moderate"))
    }

    @Test
    fun testMapToForecastUi_WithRealFixtures() {
        val forecastStream = javaClass.classLoader?.getResourceAsStream("openmeteo-forecast.json")
        assertNotNull("Fixture openmeteo-forecast.json must exist", forecastStream)
        val forecastJson = forecastStream!!.bufferedReader().readText()
        val forecastDto = json.decodeFromString<OpenMeteoForecastResponse>(forecastJson)

        val aqiStream = javaClass.classLoader?.getResourceAsStream("openmeteo-air-quality.json")
        assertNotNull("Fixture openmeteo-air-quality.json must exist", aqiStream)
        val aqiJson = aqiStream!!.bufferedReader().readText()
        val aqiDto = json.decodeFromString<OpenMeteoAirQualityResponse>(aqiJson)

        val ui = ForecastMapper.mapToForecastUi(
            forecastResponse = forecastDto,
            airQualityResponse = aqiDto,
            locationLabel = "Mumbai, Maharashtra",
            timestampMs = 1727515800000L
        )

        assertEquals("Mumbai, Maharashtra", ui.locationLabel)
        assertEquals("Weather data by Open-Meteo.com", ui.attribution)
        assertEquals("31°", ui.current.temperatureFormatted)
        assertEquals(82, ui.current.aqi)
        assertEquals(7, ui.days.size)
        assertTrue(ui.hourly.isNotEmpty())
        assertEquals("Now", ui.hourly.first().timeLabel)
    }
}
