package com.weatherengine.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PersonaDto(
    val code: String,
    val displayName: String
)

@Serializable
data class SetPersonasRequest(
    val personaCodes: List<String>
)
