package com.weatherengine.backend.persona

import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PersonaService(
    private val personaRepository: PersonaRepository,
    private val personaWidgetRepository: PersonaWidgetRepository,
    private val userPersonaRepository: UserPersonaRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getUserPersonas(userId: UUID): List<PersonaDto> {
        return userPersonaRepository.findByUserId(userId)
            .map { userPersona ->
                PersonaDto(
                    code = userPersona.persona.code,
                    displayName = userPersona.persona.displayName
                )
            }
    }

    @Transactional
    fun setUserPersonas(userId: UUID, requestedCodes: List<String>): List<PersonaDto> {
        val distinctCodes = requestedCodes.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinctCodes.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Please select at least one persona")
        }

        val user = userRepository.findById(userId).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "User account not found")
        }

        val foundPersonas = personaRepository.findByCodeIn(distinctCodes)
        val foundByCode = foundPersonas.associateBy { it.code }

        for (code in distinctCodes) {
            if (!foundByCode.containsKey(code)) {
                throw ApiException(HttpStatus.BAD_REQUEST, "Invalid persona code: $code")
            }
        }

        userPersonaRepository.deleteByUserId(userId)
        userPersonaRepository.flush()

        val orderedPersonas = distinctCodes.mapNotNull { foundByCode[it] }
        val newMappings = orderedPersonas.mapNotNull { persona ->
            val pId = persona.id ?: return@mapNotNull null
            UserPersona(
                id = UserPersonaId(userId = userId, personaId = pId),
                user = user,
                persona = persona
            )
        }
        userPersonaRepository.saveAll(newMappings)

        return orderedPersonas.map { PersonaDto(code = it.code, displayName = it.displayName) }
    }

    @Transactional(readOnly = true)
    fun getUserWidgetCodes(userId: UUID): List<String> {
        val personaIds = userPersonaRepository.findByUserId(userId)
            .mapNotNull { it.persona.id }
        if (personaIds.isEmpty()) {
            return emptyList()
        }
        return personaWidgetRepository.findByPersonaIdInOrderByDisplayOrderAsc(personaIds)
            .map { it.widgetCode }
            .distinct()
    }
}
