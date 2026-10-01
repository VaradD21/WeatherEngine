package com.weatherengine.backend.persona

import com.weatherengine.backend.user.User
import jakarta.persistence.*
import java.io.Serializable
import java.util.UUID

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
