package com.weatherengine.backend.user

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_preferences")
class UserPreferences(
    @Id
    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "widget_order", columnDefinition = "TEXT")
    var widgetOrder: String? = null,

    @Column(name = "disabled_widgets", columnDefinition = "TEXT")
    var disabledWidgets: String? = null,

    @Column(name = "default_screen")
    var defaultScreen: String = "home",

    @Column(name = "temperature_unit")
    var temperatureUnit: String = "CELSIUS",

    @Column(name = "school_start")
    var schoolStart: String = "08:00",

    @Column(name = "school_end")
    var schoolEnd: String = "15:00",

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)

interface UserPreferencesRepository : JpaRepository<UserPreferences, UUID>
