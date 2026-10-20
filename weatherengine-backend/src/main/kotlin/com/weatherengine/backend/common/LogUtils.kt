package com.weatherengine.backend.common

import kotlin.math.round

/**
 * Rounds a coordinate to 1 decimal place for log output.
 * Prevents precise user coordinates appearing in log files.
 *
 * Example: 19.0752 -> "19.1"
 */
fun Double.roundForLog(): String = "%.1f".format(round(this * 10.0) / 10.0)
