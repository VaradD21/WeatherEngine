package com.weatherengine.app.core

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
}
