package com.weatherengine.backend.user

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.PersonaService
import com.weatherengine.backend.weather.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpStatus
import java.util.Optional
import java.util.UUID

class UserPreferencesTest {

    private val preferencesRepository = mockk<UserPreferencesRepository>()
    private val controller = UserPreferencesController(preferencesRepository)

    @Test
    fun `getPreferences returns default values when no record exists`() {
        val userId = UUID.randomUUID()
        every { preferencesRepository.findById(userId) } returns Optional.empty()

        val response = controller.getPreferences(AuthenticatedUser(userId, "test@example.com"))

        assertNotNull(response.body)
        assertEquals("home", response.body?.defaultScreen)
        assertEquals("CELSIUS", response.body?.temperatureUnit)
        assertTrue(response.body!!.widgetOrder.isEmpty())
        assertTrue(response.body!!.disabledWidgets.isEmpty())
    }

    @Test
    fun `updatePreferences saves and returns custom widgetOrder and defaultScreen`() {
        val userId = UUID.randomUUID()
        val existing = UserPreferences(userId = userId)
        every { preferencesRepository.findById(userId) } returns Optional.of(existing)
        every { preferencesRepository.save(any()) } answers { firstArg() }

        val request = UserPreferencesDto(
            widgetOrder = listOf("aqi_card", "traffic_card", "soil_moisture_card"),
            disabledWidgets = setOf("uv_index_card"),
            defaultScreen = "commute",
            temperatureUnit = "CELSIUS",
            schoolStart = "07:30",
            schoolEnd = "14:30"
        )

        val response = controller.updatePreferences(AuthenticatedUser(userId, "test@example.com"), request)

        assertNotNull(response.body)
        assertEquals("commute", response.body?.defaultScreen)
        assertEquals(listOf("aqi_card", "traffic_card", "soil_moisture_card"), response.body?.widgetOrder)
        assertEquals(setOf("uv_index_card"), response.body?.disabledWidgets)
        assertEquals("07:30", response.body?.schoolStart)
    }

    // ── STEP 2: school-hours validation on PUT ──────────────────────────────

    @ParameterizedTest(name = "bad format ''{0}'' / ''{1}'' -> 400")
    @CsvSource(
        "8:00,   15:00",   // missing leading zero
        "08:60,  15:00",   // invalid minute
        "25:00,  15:00",   // invalid hour
        "abc,    15:00",   // garbage
        "08:00,  def",     // garbage end
        "'',     15:00"    // blank start
    )
    fun `updatePreferences rejects bad schoolStart format with 400`(start: String?, end: String?) {
        val userId = UUID.randomUUID()
        every { preferencesRepository.findById(userId) } returns Optional.of(UserPreferences(userId = userId))

        val dto = UserPreferencesDto(schoolStart = start?.trim() ?: "", schoolEnd = end?.trim() ?: "")
        val ex = assertThrows<ApiException> {
            controller.updatePreferences(AuthenticatedUser(userId, "u@e.com"), dto)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    @ParameterizedTest(name = "end<=''{1}'' <= start ''{0}'' -> 400")
    @CsvSource(
        "15:00, 08:00",   // end before start
        "10:00, 10:00"    // end equals start
    )
    fun `updatePreferences rejects schoolEnd not after schoolStart with 400`(start: String, end: String) {
        val userId = UUID.randomUUID()
        every { preferencesRepository.findById(userId) } returns Optional.of(UserPreferences(userId = userId))

        val dto = UserPreferencesDto(schoolStart = start, schoolEnd = end)
        val ex = assertThrows<ApiException> {
            controller.updatePreferences(AuthenticatedUser(userId, "u@e.com"), dto)
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
        assertTrue(ex.message.contains("strictly after", ignoreCase = true))
    }

    @Test
    fun `updatePreferences accepts valid school hours and persists them`() {
        val userId = UUID.randomUUID()
        val existing = UserPreferences(userId = userId)
        every { preferencesRepository.findById(userId) } returns Optional.of(existing)
        every { preferencesRepository.save(any()) } answers { firstArg() }

        val dto = UserPreferencesDto(schoolStart = "08:30", schoolEnd = "14:45")
        val response = controller.updatePreferences(AuthenticatedUser(userId, "u@e.com"), dto)

        assertEquals("08:30", response.body?.schoolStart)
        assertEquals("14:45", response.body?.schoolEnd)
    }

    // ── STEP 2: homepage fallback for corrupted stored hours ────────────────

    @Test
    fun `WeatherController homepage falls back to defaults when stored school hours are corrupt`() {
        val weatherCacheService = mockk<WeatherCacheService>()
        val personaService = mockk<PersonaService>()
        val widgetBuilder = mockk<WidgetBuilder>()
        val prefsRepo = mockk<UserPreferencesRepository>()

        val weatherController = WeatherController(
            weatherCacheService = weatherCacheService,
            personaService = personaService,
            widgetBuilder = widgetBuilder,
            preferencesRepository = prefsRepo
        )

        val userId = UUID.randomUUID()
        val principal = AuthenticatedUser(userId, "test@example.com")

        // Stored values are corrupt (bad format saved before validation was added)
        val corruptPrefs = UserPreferences(
            userId = userId,
            schoolStart = "GARBAGE",
            schoolEnd = "ALSO_BAD"
        )
        every { prefsRepo.findById(userId) } returns Optional.of(corruptPrefs)
        every { personaService.getUserWidgetCodes(userId) } returns listOf("school_commute_card")

        val bundle = WeatherBundleDto(forecast = OpenMeteoForecastResponse(latitude = 19.07, longitude = 72.87))
        every { weatherCacheService.getCachedWeatherBundle(19.07, 72.87) } returns bundle

        // widgetBuilder should be called with the fallback defaults (08:00 / 15:00), not the corrupt values
        var capturedStart = ""
        var capturedEnd = ""
        every { widgetBuilder.buildWidget(any(), any(), any(), any()) } answers {
            capturedStart = arg(2)
            capturedEnd = arg(3)
            WidgetDto(type = firstArg(), data = mapOf("status" to "ok"))
        }

        // Should NOT throw — must return 200 with fallback hours
        val response = weatherController.getHomepage(
            principal = principal,
            lat = 19.07,
            lon = 72.87,
            schoolStart = "08:00",
            schoolEnd = "15:00"
        )

        assertEquals(200, response.statusCode.value())
        // Widget was built with defaults, not the corrupt stored values
        assertEquals("08:00", capturedStart)
        assertEquals("15:00", capturedEnd)
    }

    @Test
    fun `WeatherController personalizes homepage using cloud widgetOrder and disabledWidgets`() {
        val weatherCacheService = mockk<WeatherCacheService>()
        val personaService = mockk<PersonaService>()
        val widgetBuilder = mockk<WidgetBuilder>()
        val prefsRepo = mockk<UserPreferencesRepository>()

        val weatherController = WeatherController(
            weatherCacheService = weatherCacheService,
            personaService = personaService,
            widgetBuilder = widgetBuilder,
            preferencesRepository = prefsRepo
        )

        val userId = UUID.randomUUID()
        val principal = AuthenticatedUser(userId, "test@example.com")

        every { personaService.getUserWidgetCodes(userId) } returns listOf("aqi_card", "humidity_card", "uv_index_card", "traffic_card")

        val userPrefs = UserPreferences(
            userId = userId,
            widgetOrder = "traffic_card,aqi_card",
            disabledWidgets = "uv_index_card",
            defaultScreen = "commute"
        )
        every { prefsRepo.findById(userId) } returns Optional.of(userPrefs)

        val bundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                latitude = 19.07,
                longitude = 72.87,
                current = OpenMeteoCurrentWeather(weatherCode = 0, apparentTemperature = 25.0)
            )
        )
        every { weatherCacheService.getCachedWeatherBundle(19.07, 72.87) } returns bundle

        every { widgetBuilder.buildWidget(any(), any(), any(), any()) } answers {
            WidgetDto(type = firstArg(), data = mapOf("status" to "ok"))
        }

        val response = weatherController.getHomepage(
            principal = principal,
            lat = 19.07,
            lon = 72.87,
            schoolStart = "08:00",
            schoolEnd = "15:00"
        )

        assertNotNull(response.body)
        val widgets = response.body?.widgets ?: emptyList()

        assertFalse(widgets.any { it.type == "uv_index_card" })
        assertEquals("traffic_card", widgets[0].type)
        assertEquals("aqi_card", widgets[1].type)
        assertEquals("humidity_card", widgets[2].type)
        assertNotNull(response.body?.preferences)
        assertEquals("commute", response.body?.preferences?.defaultScreen)
    }
}
