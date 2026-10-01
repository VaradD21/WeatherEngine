package com.weatherengine.backend.persona

import jakarta.validation.constraints.NotEmpty

data class PersonaDto(
    val code: String,
    val displayName: String
)

data class SetPersonasRequest(
    @field:NotEmpty(message = "Please select at least one persona")
    val personaCodes: List<String>
)
