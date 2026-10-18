package com.weatherengine.backend.location

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "user_location_history",
    indexes = [
        Index(name = "idx_user_location_history_user_time", columnList = "user_id, recorded_at DESC"),
        Index(name = "idx_user_location_routine", columnList = "user_id, day_of_week, hour_of_day")
    ]
)
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

interface UserLocationHistoryRepository : JpaRepository<UserLocationHistory, Long> {

    fun findTopByUserIdOrderByRecordedAtDesc(userId: UUID): UserLocationHistory?

    fun findTop50ByUserIdOrderByRecordedAtDesc(userId: UUID): List<UserLocationHistory>

    fun findByUserIdAndDayOfWeekAndHourOfDay(
        userId: UUID,
        dayOfWeek: Int,
        hourOfDay: Int
    ): List<UserLocationHistory>

    @Modifying
    @Query("DELETE FROM UserLocationHistory u WHERE u.recordedAt < :cutoff")
    fun deleteOlderThan(@Param("cutoff") cutoff: Instant): Int
}
