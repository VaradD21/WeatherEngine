package com.weatherengine.app.core

import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    fun formatHourCompact(isoString: String): String {
        return try {
            val dt = LocalDateTime.parse(isoString)
            dt.format(DateTimeFormatter.ofPattern("h a", Locale.US))
        } catch (_: Exception) {
            isoString
        }
    }

    fun minutesAgo(timestampMs: Long, nowMs: Long): Long {
        val diffMs = nowMs - timestampMs
        if (diffMs <= 0) return 0L
        return diffMs / 60000L
    }
}

object UrlUtils {
    /**
     * Normalizes base URL.
     * Trims, strips trailing slashes, accepts only http:// or https://,
     * returns null for blank or invalid schemes ("javascript:", "ftp://", "localhost:8080").
     */
    fun normalizeBaseUrl(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        val lower = trimmed.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return null
        }

        val cleaned = trimmed.replace(Regex("/+$"), "")
        return try {
            val uri = URI(cleaned)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                return null
            }
            if (uri.host.isNullOrBlank()) {
                null
            } else {
                cleaned
            }
        } catch (_: Exception) {
            null
        }
    }
}

object Validators {
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return emailPattern.matches(email.trim())
    }

    fun isValidPassword(password: String?): Boolean {
        if (password == null) return false
        if (password.trim().isEmpty()) return false
        return password.length >= 8
    }

    fun isValidLatLon(lat: Double?, lon: Double?): Boolean {
        if (lat == null || lon == null) return false
        if (lat.isNaN() || lon.isNaN() || lat.isInfinite() || lon.isInfinite()) return false
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }

    fun isValidLatLon(latStr: String?, lonStr: String?): Boolean {
        if (latStr.isNullOrBlank() || lonStr.isNullOrBlank()) return false
        val lat = latStr.toDoubleOrNull()
        val lon = lonStr.toDoubleOrNull()
        return isValidLatLon(lat, lon)
    }

    fun validateSchoolHours(startStr: String?, endStr: String?): String? {
        val s = startStr?.trim().orEmpty()
        val e = endStr?.trim().orEmpty()
        val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        if (!timeRegex.matches(s) || !timeRegex.matches(e)) {
            return "Both times must be in HH:mm 24-hour format (e.g. 08:00, 15:00)"
        }
        val (sH, sM) = s.split(":").map { it.toInt() }
        val (eH, eM) = e.split(":").map { it.toInt() }
        val startMins = sH * 60 + sM
        val endMins = eH * 60 + eM
        if (endMins <= startMins) {
            return "School end time must be strictly after school start time"
        }
        return null
    }
}

