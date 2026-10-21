package com.weatherengine.backend.alert

import com.weatherengine.backend.location.Location
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "alert_log")
class AlertLog(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = true)
    var location: Location? = null,

    @Column(name = "user_id")
    var userId: UUID? = null,

    @Column(name = "alert_type", nullable = false)
    var alertType: String,

    @Column(name = "headline")
    var headline: String? = null,

    @Column(name = "message", columnDefinition = "TEXT")
    var message: String? = null,

    @Column(name = "severity")
    var severity: String = "WARNING",

    @Column(name = "latitude")
    var latitude: Double? = null,

    @Column(name = "longitude")
    var longitude: Double? = null,

    @Column(name = "sent_at")
    var sentAt: Instant = Instant.now()
)

data class AlertLogDto(
    val id: UUID?,
    val userId: UUID?,
    val locationId: UUID?,
    val alertType: String,
    val headline: String?,
    val message: String?,
    val severity: String,
    val latitude: Double?,
    val longitude: Double?,
    val sentAt: Instant
)

fun AlertLog.toDto(): AlertLogDto = AlertLogDto(
    id = this.id,
    userId = this.userId,
    locationId = this.location?.id,
    alertType = this.alertType,
    headline = this.headline,
    message = this.message,
    severity = this.severity,
    latitude = this.latitude ?: this.location?.latitude?.toDouble(),
    longitude = this.longitude ?: this.location?.longitude?.toDouble(),
    sentAt = this.sentAt
)

interface AlertLogRepository : JpaRepository<AlertLog, UUID> {
    fun findTop50ByOrderBySentAtDesc(): List<AlertLog>
    fun findTop20ByUserIdOrderBySentAtDesc(userId: UUID): List<AlertLog>
}
