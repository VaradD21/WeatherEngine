package com.weatherengine.backend.persona

import jakarta.persistence.*

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
    var widgetCode: String, // e.g. "aqi_card", "tide_card"

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int
)
