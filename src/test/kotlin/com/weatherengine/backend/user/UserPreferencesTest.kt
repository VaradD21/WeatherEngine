package com.weatherengine.backend.user

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.persona.PersonaService
import com.weatherengine.backend.weather.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
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

        // uv_index_card must be excluded because it is disabled
        assertFalse(widgets.any { it.type == "uv_index_card" })

        // traffic_card and aqi_card must come first according to customOrder
        assertEquals("traffic_card", widgets[0].type)
        assertEquals("aqi_card", widgets[1].type)
        assertEquals("humidity_card", widgets[2].type)

        // preferences should be populated in the response
        assertNotNull(response.body?.preferences)
        assertEquals("commute", response.body?.preferences?.defaultScreen)
    }
}
