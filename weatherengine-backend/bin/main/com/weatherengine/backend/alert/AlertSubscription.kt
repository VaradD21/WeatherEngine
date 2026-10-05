package com.weatherengine.backend.alert

import com.weatherengine.backend.location.Location
import com.weatherengine.backend.user.User
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "alert_subscriptions")
class AlertSubscription(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    var location: Location,

    @Column(name = "fcm_token", nullable = false)
    var fcmToken: String,

    var active: Boolean = true
)
