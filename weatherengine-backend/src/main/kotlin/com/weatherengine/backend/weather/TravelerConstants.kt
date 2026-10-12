package com.weatherengine.backend.weather

object TravelerConstants {
    // Flight weather hazard thresholds
    const val FLIGHT_SEVERE_WIND_GUST_KMH = 55.0
    const val FLIGHT_DENSE_FOG_VISIBILITY_METERS = 1000.0
    const val FLIGHT_LOW_VISIBILITY_METERS = 3000.0
    const val FLIGHT_HEAVY_PRECIP_PROBABILITY = 70

    // Packing temperature thresholds (°C)
    const val TEMP_FREEZING_MAX = 5.0
    const val TEMP_COOL_MAX = 16.0
    const val TEMP_COMFORTABLE_MAX = 25.0
    const val TEMP_HOT_MIN = 28.0

    // UV & Rain thresholds for packing
    const val PACKING_RAIN_CHANCE_MIN = 30
    const val PACKING_HIGH_UV_MIN = 6.0
}
