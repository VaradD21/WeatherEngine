package com.weatherengine.backend.persona

import jakarta.persistence.*

@Entity
@Table(name = "personas")
class Persona(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(unique = true, nullable = false)
    var code: String, // e.g. "health_conscious"

    @Column(name = "display_name", nullable = false)
    var displayName: String
)
