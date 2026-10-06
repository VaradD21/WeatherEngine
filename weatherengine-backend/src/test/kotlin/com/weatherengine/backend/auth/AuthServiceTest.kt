package com.weatherengine.backend.auth

import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.Persona
import com.weatherengine.backend.persona.PersonaRepository
import com.weatherengine.backend.persona.PersonaService
import com.weatherengine.backend.persona.PersonaWidget
import com.weatherengine.backend.persona.PersonaWidgetRepository
import com.weatherengine.backend.persona.UserPersona
import com.weatherengine.backend.persona.UserPersonaId
import com.weatherengine.backend.persona.UserPersonaRepository
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserRepository
import com.weatherengine.backend.weather.OpenMeteoAirQualityResponse
import com.weatherengine.backend.weather.OpenMeteoCurrentAirQuality
import com.weatherengine.backend.weather.OpenMeteoCurrentWeather
import com.weatherengine.backend.weather.OpenMeteoForecastResponse
import com.weatherengine.backend.weather.OpenMeteoHourlyForecast
import com.weatherengine.backend.weather.WeatherBundleDto
import com.weatherengine.backend.weather.WidgetBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val personaRepository = mockk<PersonaRepository>()
    private val userPersonaRepository = mockk<UserPersonaRepository>(relaxed = true)
    private val passwordEncoder = BCryptPasswordEncoder()
    private val jwtService = JwtService(
        secret = "test-secret-key-must-be-at-least-32-bytes-long-12345",
        expirationMs = 3_600_000L
    )

    private val authService = AuthService(
        userRepository = userRepository,
        personaRepository = personaRepository,
        userPersonaRepository = userPersonaRepository,
        passwordEncoder = passwordEncoder,
        jwtService = jwtService
    )

    @Test
    fun `signup creates user, assigns default persona, and returns valid JWT`() {
        val userId = UUID.randomUUID()
        every { userRepository.existsByEmail("user@example.com") } returns false
        every { userRepository.save(any()) } answers {
            val u = firstArg<User>()
            User(id = userId, email = u.email, passwordHash = u.passwordHash)
        }
        every { personaRepository.findByCode("health_conscious") } returns Persona(
            id = 1,
            code = "health_conscious",
            displayName = "Health-conscious"
        )

        val response = authService.signup(SignupRequest("User@Example.com", "password123"))

        assertEquals("user@example.com", response.email)
        val parsed = jwtService.parseToken(response.token)
        assertNotNull(parsed)
        assertEquals(userId, parsed?.userId)
        assertEquals("user@example.com", parsed?.email)
        verify(exactly = 1) { userPersonaRepository.save(any()) }
    }

    @Test
    fun `signup throws 409 CONFLICT when email already exists`() {
        every { userRepository.existsByEmail("dup@example.com") } returns true

        val ex = assertThrows(ApiException::class.java) {
            authService.signup(SignupRequest("dup@example.com", "password123"))
        }
        assertEquals(HttpStatus.CONFLICT, ex.status)
    }

    @Test
    fun `login succeeds with valid credentials and fails with wrong password`() {
        val userId = UUID.randomUUID()
        val hash = passwordEncoder.encode("correctPass123")
        every { userRepository.findByEmail("user@example.com") } returns User(
            id = userId,
            email = "user@example.com",
            passwordHash = hash
        )

        val success = authService.login(LoginRequest("user@example.com", "correctPass123"))
        assertEquals("user@example.com", success.email)
        assertNotNull(jwtService.parseToken(success.token))

        val ex = assertThrows(ApiException::class.java) {
            authService.login(LoginRequest("user@example.com", "wrongPass123"))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
    }
}

class PersonaServiceTest {

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

    @Test
    fun `setUserPersonas validates codes and replaces user personas`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = "test@example.com", passwordHash = "hash")
        val p1 = Persona(id = 1, code = "health_conscious", displayName = "Health-conscious")
        val p2 = Persona(id = 2, code = "commuter", displayName = "Commuter")

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { personaRepository.findByCodeIn(listOf("health_conscious", "commuter")) } returns listOf(p1, p2)

        val result = personaService.setUserPersonas(userId, listOf("health_conscious", "commuter"))

        assertEquals(2, result.size)
        assertEquals("health_conscious", result[0].code)
        assertEquals("commuter", result[1].code)
        verify(exactly = 1) { userPersonaRepository.deleteByUserId(userId) }
        verify(exactly = 1) { userPersonaRepository.saveAll(any<List<UserPersona>>()) }
    }

    @Test
    fun `setUserPersonas rejects unknown persona code with 400 BAD_REQUEST`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = "test@example.com", passwordHash = "hash")
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { personaRepository.findByCodeIn(listOf("invalid_code")) } returns emptyList()

        val ex = assertThrows(ApiException::class.java) {
            personaService.setUserPersonas(userId, listOf("invalid_code"))
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.status)
    }

    @Test
    fun `getUserWidgetCodes returns deduplicated ordered widget codes`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = "test@example.com", passwordHash = "hash")
        val p1 = Persona(id = 1, code = "health_conscious", displayName = "Health-conscious")

        every { userPersonaRepository.findByUserId(userId) } returns listOf(
            UserPersona(UserPersonaId(userId, 1), user, p1)
        )
        every { personaWidgetRepository.findByPersonaIdInOrderByDisplayOrderAsc(listOf(1)) } returns listOf(
            PersonaWidget(id = 10, persona = p1, widgetCode = "aqi_card", displayOrder = 1),
            PersonaWidget(id = 11, persona = p1, widgetCode = "uv_index_card", displayOrder = 2)
        )

        val codes = personaService.getUserWidgetCodes(userId)
        assertEquals(listOf("aqi_card", "uv_index_card"), codes)
    }
}

class WidgetBuilderTest {

    private val widgetBuilder = WidgetBuilder()

    @Test
    fun `buildWidget maps aqi, humidity, and heat_alert cards accurately`() {
        val bundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                current = OpenMeteoCurrentWeather(
                    temperature2m = 31.0,
                    apparentTemperature = 35.0,
                    relativeHumidity2m = 72
                ),
                hourly = OpenMeteoHourlyForecast(
                    apparentTemperature = listOf(35.0, 36.0)
                )
            ),
            airQuality = OpenMeteoAirQualityResponse(
                current = OpenMeteoCurrentAirQuality(usAqi = 42)
            )
        )

        val aqiWidget = widgetBuilder.buildWidget("aqi_card", bundle)
        assertEquals("aqi_card", aqiWidget.type)
        assertEquals(42, aqiWidget.data["aqi"])
        assertEquals("Good", aqiWidget.data["category"])

        val humidityWidget = widgetBuilder.buildWidget("humidity_card", bundle)
        assertEquals(72, humidityWidget.data["humidityPercent"])

        val heatWidget = widgetBuilder.buildWidget("heat_alert_card", bundle)
        assertEquals(true, heatWidget.data["alertActive"])
        assertEquals(35.0, heatWidget.data["temperatureCelsius"])
    }
}
