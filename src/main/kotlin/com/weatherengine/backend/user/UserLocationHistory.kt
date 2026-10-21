package com.weatherengine.backend.user

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_location_history")
class UserLocationHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "latitude", nullable = false)
    var latitude: Double,

    @Column(name = "longitude", nullable = false)
    var longitude: Double,

    @Column(name = "accuracy_meters")
    var accuracyMeters: Float? = null,

    @Column(name = "hour_of_day", nullable = false)
    var hourOfDay: Int,

    @Column(name = "day_of_week", nullable = false)
    var dayOfWeek: Int,

    @Column(name = "recorded_at", nullable = false)
    var recordedAt: Instant = Instant.now()
)

data class RecordLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val timestampMs: Long? = null
)

data class PredictiveWeatherAlertDto(
    val targetHour: Int,
    val targetTimeFormatted: String,
    val predictedLatitude: Double,
    val predictedLongitude: Double,
    val alertType: String,
    val headline: String,
    val message: String,
    val severity: String = "WARNING"
)

data class LocationRecordResponse(
    val status: String,
    val isNewSlice: Boolean,
    val latitude: Double,
    val longitude: Double,
    val recordedAt: String,
    val predictedAlert: PredictiveWeatherAlertDto? = null
)

interface UserLocationHistoryRepository : JpaRepository<UserLocationHistory, Long> {
    fun findTop10ByUserIdOrderByRecordedAtDesc(userId: UUID): List<UserLocationHistory>
    fun findTop1ByUserIdOrderByRecordedAtDesc(userId: UUID): UserLocationHistory?
}
