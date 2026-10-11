package com.weatherengine.backend.weather

object OutdoorFitnessConstants {
    // Running candidate window (hours of day)
    const val RUN_CANDIDATE_START_HOUR = 5
    const val RUN_CANDIDATE_END_HOUR = 21
    const val RUN_WINDOW_CONSECUTIVE_HOURS = 2
    const val RUN_MIN_ACCEPTABLE_SCORE = 55

    // Weather penalties for outdoor workout
    const val PENALTY_TEMP_FREEZING = 40
    const val PENALTY_TEMP_CHILLY = 15
    const val PENALTY_TEMP_WARM = 10
    const val PENALTY_TEMP_HOT = 25
    const val PENALTY_TEMP_VERY_HOT = 45
    const val PENALTY_TEMP_EXTREME = 70

    const val PENALTY_RAIN_LIGHT = 15
    const val PENALTY_RAIN_MODERATE = 35
    const val PENALTY_RAIN_HEAVY = 60

    const val PENALTY_WIND_MODERATE = 10
    const val PENALTY_WIND_STRONG = 25
    const val PENALTY_WIND_GALE = 50

    const val PENALTY_AQI_MODERATE = 10
    const val PENALTY_AQI_UNHEALTHY = 30
    const val PENALTY_AQI_HAZARDOUS = 60

    const val PENALTY_UV_HIGH = 15
    const val PENALTY_UV_VERY_HIGH = 30

    // Heat Alert Thresholds (°C feels-like)
    const val HEAT_ALERT_MIN = 38.0
    const val HEAT_CAUTION_MIN = 32.0
    const val HEAT_DISCLAIMER_NOTE = "App estimate, not an official IMD warning"

    // Wind speed categories (km/h)
    const val WIND_LIGHT_MAX = 15.0
    const val WIND_MODERATE_MAX = 28.0
    const val WIND_BREEZY_MAX = 40.0
}
