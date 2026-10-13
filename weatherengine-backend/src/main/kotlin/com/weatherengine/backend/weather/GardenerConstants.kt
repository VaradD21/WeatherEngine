package com.weatherengine.backend.weather

object GardenerConstants {
    // Soil moisture heuristics (no Open-Meteo free-tier soil endpoint)
    const val SOIL_WET_PRECIP_7D_MM = 40.0     // ≥ 40 mm in 7 days → wet
    const val SOIL_MOIST_PRECIP_7D_MM = 15.0   // ≥ 15 mm → moist
    const val SOIL_DRY_HUMIDITY_MAX = 40        // humidity < 40 % → dry

    // Frost thresholds
    const val FROST_RISK_TEMP_C = 2.0           // ≤ 2 °C → frost risk
    const val HARD_FROST_TEMP_C = -2.0          // ≤ -2 °C → hard frost

    // Rainfall prediction windows
    const val FORECAST_DAYS = 7

    // Planting comfort range
    const val PLANT_TEMP_MIN_C = 8.0
    const val PLANT_TEMP_MAX_C = 30.0
}
