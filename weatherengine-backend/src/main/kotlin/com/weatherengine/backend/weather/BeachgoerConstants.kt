package com.weatherengine.backend.weather

object BeachgoerConstants {
    // Wave height thresholds (meters)
    const val WAVE_FLAT_MAX = 0.5
    const val WAVE_CHOPPY_MAX = 1.2
    const val WAVE_MODERATE_MAX = 2.0
    const val WAVE_HIGH_MAX = 3.0

    // Water temperature comfort (°C)
    const val WATER_WARM_MIN = 24.0
    const val WATER_COMFORTABLE_MIN = 20.0
    const val WATER_COOL_MIN = 16.0

    // Surf suitability scale
    const val SURF_IDEAL_PERIOD_SEC = 9.0
}
