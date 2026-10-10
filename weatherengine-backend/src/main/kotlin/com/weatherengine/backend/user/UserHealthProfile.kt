package com.weatherengine.backend.user

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_health_profiles")
class UserHealthProfile(
    @Id
    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "has_asthma")
    var hasAsthma: Boolean = false,

    @Column(name = "has_allergies")
    var hasAllergies: Boolean = false,

    @Column(name = "has_skin_sensitivity")
    var hasSkinSensitivity: Boolean = false,

    @Column(name = "aqi_threshold")
    var aqiThreshold: Int = 100,

    @Column(name = "uv_threshold")
    var uvThreshold: Int = 6,

    @Column(name = "humidity_threshold")
    var humidityThreshold: Int = 70,

    @Column(name = "pollen_threshold")
    var pollenThreshold: String = "MODERATE",

    @Column(name = "alerts_enabled")
    var alertsEnabled: Boolean = true,

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)

interface UserHealthProfileRepository : JpaRepository<UserHealthProfile, UUID>
