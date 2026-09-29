package com.weatherengine.app.core

import java.net.URI

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
