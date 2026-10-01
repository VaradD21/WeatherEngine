package com.weatherengine.backend.persona

import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.Optional
import java.util.UUID

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
