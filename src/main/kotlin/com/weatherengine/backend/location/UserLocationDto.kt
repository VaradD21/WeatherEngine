package com.weatherengine.backend.location

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull

data class RecordLocationRequest(
    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @field:DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    val latitude: Double,

    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @field:DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
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

data class LocationHistoryItemDto(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val recordedAt: String
)
