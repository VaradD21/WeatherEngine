package com.weatherengine.backend.user

import com.weatherengine.backend.auth.AuthenticatedUser
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.*

class UserPreferencesTest {

    private val preferencesRepository = mockk<UserPreferencesRepository>()
    private val controller = UserPreferencesController(preferencesRepository)

    private val userId = UUID.randomUUID()
    private val authUser = AuthenticatedUser(userId, "prefuser@example.com")

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `getPreferences returns default preferences when none exist in DB`() {
        every { preferencesRepository.findById(userId) } returns Optional.empty()

        val response = controller.getPreferences(authUser)
        assertEquals(HttpStatus.OK, response.statusCode)
        val dto = response.body
        assertNotNull(dto)
        assertEquals("home", dto?.defaultScreen)
        assertEquals("CELSIUS", dto?.temperatureUnit)
        assertEquals("08:00", dto?.schoolStart)
        assertEquals("15:00", dto?.schoolEnd)
        assertTrue(dto?.widgetOrder?.isEmpty() == true)
        assertTrue(dto?.disabledWidgets?.isEmpty() == true)
    }

    @Test
    fun `updatePreferences saves and returns sanitized preferences`() {
        val existing = UserPreferences(userId = userId)
        every { preferencesRepository.findById(userId) } returns Optional.of(existing)
        val savedSlot = slot<UserPreferences>()
        every { preferencesRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val request = UserPreferencesDto(
            widgetOrder = listOf("aqi_card", "uv_index_card"),
            disabledWidgets = setOf("pollen_card"),
            defaultScreen = "forecast",
            temperatureUnit = "FAHRENHEIT",
            schoolStart = "07:30",
            schoolEnd = "14:30"
        )

        val response = controller.updatePreferences(authUser, request)
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(listOf("aqi_card", "uv_index_card"), body?.widgetOrder)
        assertEquals(setOf("pollen_card"), body?.disabledWidgets)
        assertEquals("FAHRENHEIT", body?.temperatureUnit)
        assertEquals("07:30", body?.schoolStart)
        assertEquals("14:30", body?.schoolEnd)
    }

    @Test
    fun `WeatherController personalizes homepage using cloud widgetOrder and disabledWidgets`() {
        val weatherCacheService = mockk<com.weatherengine.backend.weather.WeatherCacheService>()
        val personaService = mockk<com.weatherengine.backend.persona.PersonaService>()
        val widgetBuilder = mockk<com.weatherengine.backend.weather.WidgetBuilder>()
        val prefsRepo = mockk<UserPreferencesRepository>()

        val weatherController = com.weatherengine.backend.weather.WeatherController(
            weatherCacheService = weatherCacheService,
            personaService = personaService,
            widgetBuilder = widgetBuilder,
            preferencesRepository = prefsRepo
        )

        val uId = UUID.randomUUID()
        val principal = AuthenticatedUser(uId, "test@example.com")

        every { personaService.getUserWidgetCodes(uId) } returns listOf("aqi_card", "humidity_card", "uv_index_card", "traffic_card")

        val userPrefs = UserPreferences(
            userId = uId,
            widgetOrder = "traffic_card,aqi_card",
            disabledWidgets = "uv_index_card",
            defaultScreen = "commute"
        )
        every { prefsRepo.findById(uId) } returns Optional.of(userPrefs)

        val bundle = com.weatherengine.backend.weather.WeatherBundleDto(
            forecast = com.weatherengine.backend.weather.OpenMeteoForecastResponse(
                latitude = 19.07,
                longitude = 72.87,
                current = com.weatherengine.backend.weather.OpenMeteoCurrentWeather(weatherCode = 0, apparentTemperature = 25.0)
            )
        )
        every { weatherCacheService.getCachedWeatherBundle(19.07, 72.87) } returns bundle

        every { widgetBuilder.buildWidget(any(), any(), any(), any()) } answers {
            com.weatherengine.backend.weather.WidgetDto(type = firstArg(), data = mapOf("status" to "ok"))
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
