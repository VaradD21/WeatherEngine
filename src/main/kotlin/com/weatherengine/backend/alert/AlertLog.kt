package com.weatherengine.backend.alert

import com.weatherengine.backend.location.Location
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "alert_log")
class AlertLog(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    var location: Location,

    @Column(name = "alert_type", nullable = false)
    var alertType: String,

    @Column(name = "sent_at")
    var sentAt: Instant = Instant.now()
)
