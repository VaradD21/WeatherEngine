package com.weatherengine.backend.weather

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeatherFreshnessAndFreshBundleTest {

    @Test
    fun `freshly fetched bundle has isStale false and ttlSeconds around 900`() {
        val bundle = WeatherBundleDto(fetchedAtMs = System.currentTimeMillis())

        assertFalse(bundle.isStale, "Fresh bundle should not be stale")
        assertTrue(bundle.ttlSeconds in 890L..900L, "TTL seconds should be close to 900s, was: ${bundle.ttlSeconds}")
    }

    @Test
    fun `mock aged bundle older than 15m has isStale true and ttlSeconds 0`() {
        val sixteenMinutesAgo = System.currentTimeMillis() - (16 * 60 * 1000L)
        val agedBundle = WeatherBundleDto(fetchedAtMs = sixteenMinutesAgo)

        assertTrue(agedBundle.isStale, "Aged bundle (>15m) must be marked stale")
        assertEquals(0L, agedBundle.ttlSeconds, "Aged bundle must have 0 TTL seconds")
    }
}
