package com.weatherengine.backend.alert

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface AlertSubscriptionRepository : JpaRepository<AlertSubscription, UUID> {
    fun findByActiveTrue(): List<AlertSubscription>
}

interface AlertLogRepository : JpaRepository<AlertLog, UUID> {
    fun findByLocationIdAndAlertTypeAndSentAtAfter(
        locationId: UUID,
        alertType: String,
        after: Instant
    ): List<AlertLog>
}
