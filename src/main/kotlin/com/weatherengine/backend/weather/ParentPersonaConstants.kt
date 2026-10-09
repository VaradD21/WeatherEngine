package com.weatherengine.backend.weather

object ParentPersonaConstants {
    // School Hours Defaults
    const val DEFAULT_SCHOOL_START = "08:00"
    const val DEFAULT_SCHOOL_END = "15:00"
    const val RUN_WINDOW_DURATION_MINUTES = 60L

    // Commute Thresholds - Caution (Worst)
    val THUNDERSTORM_CODES = setOf(95, 96, 99)
    const val COMMUTE_CAUTION_PRECIP_MM = 7.5
    const val COMMUTE_CAUTION_GUST_KMH = 50.0
    const val COMMUTE_CAUTION_VISIBILITY_METERS = 1000.0
    const val COMMUTE_CAUTION_APPARENT_HOT = 38.0
    const val COMMUTE_CAUTION_APPARENT_COLD = 0.0

    // Commute Thresholds - Prepare
    const val COMMUTE_PREPARE_RAIN_PROB = 40
    const val COMMUTE_PREPARE_PRECIP_MM = 0.5
    const val COMMUTE_PREPARE_GUST_KMH = 30.0
    const val COMMUTE_PREPARE_APPARENT_HOT = 32.0
    const val COMMUTE_PREPARE_APPARENT_COLD = 8.0
    const val COMMUTE_PREPARE_UV_INDEX = 6.0

    // Rain Alert Lookahead & Thresholds
    const val RAIN_ALERT_LOOKAHEAD_HOURS = 6
    const val RAIN_ALERT_HIGH_PROB = 50
    const val RAIN_ALERT_HIGH_PRECIP_MM = 0.5
    const val RAIN_ALERT_LIGHT_PROB_MIN = 20
    const val RAIN_ALERT_LIGHT_PROB_MAX = 49

    // Severe Weather Lookahead & Thresholds
    const val SEVERE_LOOKAHEAD_HOURS = 24
    val SEVERE_HEAVY_RAIN_CODES = setOf(65, 82)
    val SEVERE_FOG_CODES = setOf(45, 48)
    const val SEVERE_HEAVY_RAIN_MM = 7.5
    const val SEVERE_WIND_GUSTS_KMH = 50.0
    const val SEVERE_LOW_VISIBILITY_METERS = 1000.0
    const val SEVERE_EXTREME_HEAT_APPARENT = 40.0
    const val SEVERE_DISCLAIMER = "App estimate, not an official IMD warning"
}
