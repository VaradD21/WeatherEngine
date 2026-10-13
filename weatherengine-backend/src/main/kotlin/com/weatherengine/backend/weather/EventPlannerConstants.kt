package com.weatherengine.backend.weather

object EventPlannerConstants {
    // Hours considered "event time" for afternoon outdoor events
    const val EVENT_HOUR_START = 15   // 3 PM
    const val EVENT_HOUR_END = 19     // 7 PM

    // Comfort index thresholds (0–100 scale)
    const val COMFORT_GREAT_MIN = 75
    const val COMFORT_GOOD_MIN = 50
    const val COMFORT_FAIR_MIN = 30

    // Temperature comfort band
    const val TEMP_COMFORTABLE_MIN_C = 18.0
    const val TEMP_COMFORTABLE_MAX_C = 28.0

    // Rain probability threshold for "likely rain"
    const val RAIN_LIKELY_PERCENT = 50

    // Extended forecast days
    const val FORECAST_DAYS = 7
}
