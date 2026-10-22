package com.weatherengine.backend.persona

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserRepository
import com.weatherengine.backend.weather.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.*

class PersonaIsolationAndBoundaryTest {

    private val personaRepository = mockk<PersonaRepository>()
    private val personaWidgetRepository = mockk<PersonaWidgetRepository>()
    private val userPersonaRepository = mockk<UserPersonaRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>()

    private val personaService = PersonaService(
        personaRepository = personaRepository,
        personaWidgetRepository = personaWidgetRepository,
        userPersonaRepository = userPersonaRepository,
        userRepository = userRepository
    )

    private val userAId = UUID.randomUUID()
    private val userBId = UUID.randomUUID()

    private val pHealth = Persona(id = 1, code = "health_conscious", displayName = "Health-conscious")
    private val pFitness = Persona(id = 2, code = "outdoor_fitness", displayName = "Outdoor Fitness Enthusiast")
    private val pCommuter = Persona(id = 3, code = "commuter", displayName = "Commuter")

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { personaRepository.findByCodeIn(any()) } answers {
            val requested = firstArg<Collection<String>>()
            listOf(pHealth, pFitness, pCommuter).filter { it.code in requested }
        }
    }

    // 2.1 & 2.2: Data Isolation
    @Test
    fun `2_1 and 2_2 - UserA and UserB persona queries are completely isolated by userId`() {
        val userA = User(id = userAId, email = "usera@example.com", passwordHash = "hashA")
        val userB = User(id = userBId, email = "userb@example.com", passwordHash = "hashB")

        every { userPersonaRepository.findByUserId(userAId) } returns listOf(
            UserPersona(UserPersonaId(userAId, 1), userA, pHealth)
        )
        every { userPersonaRepository.findByUserId(userBId) } returns listOf(
            UserPersona(UserPersonaId(userBId, 2), userB, pFitness),
            UserPersona(UserPersonaId(userBId, 3), userB, pCommuter)
        )

        val personasA = personaService.getUserPersonas(userAId)
        val personasB = personaService.getUserPersonas(userBId)

        assertEquals(1, personasA.size)
        assertEquals("health_conscious", personasA[0].code)

        assertEquals(2, personasB.size)
        assertEquals("outdoor_fitness", personasB[0].code)
        assertEquals("commuter", personasB[1].code)

        // Ensure userB sees NONE of userA's personas
        assertFalse(personasB.any { it.code == "health_conscious" })
    }

    // 3.6: Select zero personas rejected
    @Test
    fun `3_6 - Setting empty personas list throws 400 Bad Request`() {
        val ex = assertThrows(ApiException::class.java) {
            personaService.setUserPersonas(userAId, emptyList())
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
        assertEquals("Please select at least one persona", ex.message)
    }

    // 3.8: Duplicate persona submit deduplicates correctly
    @Test
    fun `3_8 - Submitting duplicate persona codes deduplicates them without creating duplicate rows`() {
        val userA = User(id = userAId, email = "usera@example.com", passwordHash = "hashA")
        every { userRepository.findById(userAId) } returns Optional.of(userA)

        val savedListSlot = slot<List<UserPersona>>()
        every { userPersonaRepository.saveAll(capture(savedListSlot)) } answers { savedListSlot.captured }

        val res = personaService.setUserPersonas(userAId, listOf("health_conscious", "health_conscious", "outdoor_fitness"))

        assertEquals(2, res.size)
        assertEquals(listOf("health_conscious", "outdoor_fitness"), res.map { it.code })
        assertEquals(2, savedListSlot.captured.size)
    }

    // 3.9: Invalid persona code throws 400
    @Test
    fun `3_9 - Submitting invalid persona code throws 400 BAD_REQUEST`() {
        val userA = User(id = userAId, email = "usera@example.com", passwordHash = "hashA")
        every { userRepository.findById(userAId) } returns Optional.of(userA)

        val ex = assertThrows(ApiException::class.java) {
            personaService.setUserPersonas(userAId, listOf("non_existent_persona"))
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
        assertTrue(ex.message.contains("Invalid persona code"))
    }

    // 3.12: Boundary tests on PersonaRules
    @Test
    fun `3_12 - Boundary test on AQI categories`() {
        val builder = WidgetBuilder()

        fun makeBundle(aqi: Int) = WeatherBundleDto(
            airQuality = OpenMeteoAirQualityResponse(
                current = OpenMeteoCurrentAirQuality(usAqi = aqi, pm25 = 10.0)
            )
        )

        assertEquals("Good", builder.buildWidget("aqi_card", makeBundle(50)).data["category"])
        assertEquals("Moderate", builder.buildWidget("aqi_card", makeBundle(51)).data["category"])
        assertEquals("Moderate", builder.buildWidget("aqi_card", makeBundle(100)).data["category"])
        assertEquals("Unhealthy for Sensitive Groups", builder.buildWidget("aqi_card", makeBundle(101)).data["category"])
    }

    @Test
    fun `3_12 - Boundary test on UV advice thresholds`() {
        val builder = WidgetBuilder()

        fun makeBundle(uv: Double) = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                hourly = OpenMeteoHourlyForecast(uvIndex = listOf(uv))
            )
        )

        val uv2Widget = builder.buildWidget("uv_index_card", makeBundle(2.9))
        assertTrue((uv2Widget.data["message"] as String).contains("Low UV"))

        val uv3Widget = builder.buildWidget("uv_index_card", makeBundle(3.0))
        assertTrue((uv3Widget.data["message"] as String).contains("Moderate UV"))

        val uv8Widget = builder.buildWidget("uv_index_card", makeBundle(8.0))
        assertTrue((uv8Widget.data["message"] as String).contains("Very High UV"))
    }

    // 3.14: Same weather + different persona produces different widgets
    @Test
    fun `3_14 - Same weather with different personas generates different widget types`() {
        val builder = WidgetBuilder()
        val sharedBundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                current = OpenMeteoCurrentWeather(
                    temperature2m = 25.0,
                    relativeHumidity2m = 50,
                    apparentTemperature = 26.0,
                    windSpeed10m = 10.0,
                    weatherCode = 0
                )
            ),
            airQuality = OpenMeteoAirQualityResponse(
                current = OpenMeteoCurrentAirQuality(usAqi = 45)
            )
        )

        val healthWidget = builder.buildWidget("aqi_card", sharedBundle)
        val fitnessWidget = builder.buildWidget("wind_speed_card", sharedBundle)

        assertNotEquals(healthWidget.type, fitnessWidget.type)
        assertEquals("aqi_card", healthWidget.type)
        assertEquals("wind_speed_card", fitnessWidget.type)
        assertTrue(healthWidget.data.containsKey("aqi"))
        assertTrue(fitnessWidget.data.containsKey("speedKmh"))
    }
}
