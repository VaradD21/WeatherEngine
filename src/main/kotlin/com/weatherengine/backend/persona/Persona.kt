package com.weatherengine.backend.persona

import com.weatherengine.backend.user.User
import jakarta.persistence.*
import jakarta.validation.constraints.NotEmpty
import org.springframework.data.jpa.repository.JpaRepository
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "personas")
class Persona(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(unique = true, nullable = false)
    var code: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String
)

@Entity
@Table(name = "persona_widgets")
class PersonaWidget(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    var persona: Persona,

    @Column(name = "widget_code", nullable = false)
    var widgetCode: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int
)

@Embeddable
data class UserPersonaId(
    @Column(name = "user_id")
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "persona_id")
    val personaId: Int = 0
) : Serializable

@Entity
@Table(name = "user_personas")
class UserPersona(
    @EmbeddedId
    val id: UserPersonaId,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("personaId")
    @JoinColumn(name = "persona_id")
    var persona: Persona
)

data class PersonaDto(
    val code: String,
    val displayName: String
)

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
