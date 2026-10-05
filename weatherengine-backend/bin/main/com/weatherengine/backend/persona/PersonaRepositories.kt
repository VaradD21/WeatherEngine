package com.weatherengine.backend.persona

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

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
