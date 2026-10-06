package com.weatherengine.backend.persona

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserRepository
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "personas")
class Persona(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int? = null,
    @Column(unique = true, nullable = false) var code: String,
    @Column(name = "display_name", nullable = false) var displayName: String
)

@Entity
@Table(name = "persona_widgets")
class PersonaWidget(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "persona_id", nullable = false) var persona: Persona,
    @Column(name = "widget_code", nullable = false) var widgetCode: String,
    @Column(name = "display_order", nullable = false) var displayOrder: Int
)

@Embeddable
data class UserPersonaId(
    @Column(name = "user_id") val userId: UUID = UUID.randomUUID(),
    @Column(name = "persona_id") val personaId: Int = 0
) : Serializable

@Entity
@Table(name = "user_personas")
class UserPersona(
    @EmbeddedId val id: UserPersonaId,
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id") var user: User,
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("personaId") @JoinColumn(name = "persona_id") var persona: Persona
)

data class PersonaDto(val code: String, val displayName: String)

data class SetPersonasRequest(
    @field:NotEmpty(message = "Please select at least one persona")
    val personaCodes: List<String>
)

interface PersonaRepository : JpaRepository<Persona, Int> {
    fun findByCode(code: String): Persona?
    fun findByCodeIn(codes: Collection<String>): List<Persona>
}

interface PersonaWidgetRepository : JpaRepository<PersonaWidget, Int> {
    fun findByPersonaIdInOrderByDisplayOrderAsc(personaIds: List<Int>): List<PersonaWidget>
}

interface UserPersonaRepository : JpaRepository<UserPersona, UserPersonaId> {
    fun findByUserId(userId: UUID): List<UserPersona>
    fun deleteByUserId(userId: UUID)
}

@RestController
@RequestMapping("/api/users/me/personas")
class PersonaController(private val personaService: PersonaService) {
    @GetMapping
    fun getPersonas(@AuthenticationPrincipal principal: AuthenticatedUser): ResponseEntity<List<PersonaDto>> =
        ResponseEntity.ok(personaService.getUserPersonas(principal.userId))

    @PostMapping
    fun setPersonas(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @Valid @RequestBody request: SetPersonasRequest
    ): ResponseEntity<List<PersonaDto>> =
        ResponseEntity.ok(personaService.setUserPersonas(principal.userId, request.personaCodes))
}

@Service
class PersonaService(
    private val personaRepository: PersonaRepository,
    private val personaWidgetRepository: PersonaWidgetRepository,
    private val userPersonaRepository: UserPersonaRepository,
    private val userRepository: UserRepository
) {
    @Transactional(readOnly = true)
    fun getUserPersonas(userId: UUID): List<PersonaDto> =
        userPersonaRepository.findByUserId(userId).map { PersonaDto(it.persona.code, it.persona.displayName) }

    @Transactional
    fun setUserPersonas(userId: UUID, requestedCodes: List<String>): List<PersonaDto> {
        val codes = requestedCodes.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (codes.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "Please select at least one persona")

        val user = userRepository.findById(userId).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "User account not found")
        }
        val byCode = personaRepository.findByCodeIn(codes).associateBy { it.code }
        val ordered = codes.map {
            byCode[it] ?: throw ApiException(HttpStatus.BAD_REQUEST, "Invalid persona code: $it")
        }

        userPersonaRepository.deleteByUserId(userId)
        userPersonaRepository.flush()
        userPersonaRepository.saveAll(
            ordered.map { UserPersona(UserPersonaId(userId, it.id!!), user, it) }
        )
        return ordered.map { PersonaDto(it.code, it.displayName) }
    }

    @Transactional(readOnly = true)
    fun getUserWidgetCodes(userId: UUID): List<String> {
        val ids = userPersonaRepository.findByUserId(userId).map { it.persona.id!! }
        if (ids.isEmpty()) return emptyList()
        return personaWidgetRepository.findByPersonaIdInOrderByDisplayOrderAsc(ids)
            .map { it.widgetCode }
            .distinct()
    }
}
