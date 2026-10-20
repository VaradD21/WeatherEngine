package com.weatherengine.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.weatherengine.backend.weather.OpenMeteoForecastResponse
import com.weatherengine.backend.weather.WeatherBundleDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.SerializationException

/**
 * Verifies the typed Redis serializer strategy:
 *  1. Round-trip: serialize then deserialize a WeatherBundleDto recovers the original value.
 *  2. No "@class" field is written (no polymorphic type metadata in the payload).
 *  3. A payload that carries an "@class" gadget-style field is safely ignored — the
 *     typed deserializer does NOT instantiate it; it raises SerializationException or
 *     silently ignores the unknown property (JsonIgnoreProperties on all DTOs).
 */
class RedisCacheSerializerTest {

    private val mapper = ObjectMapper().registerKotlinModule()
    // This is the same serializer the production RedisCacheConfig wires up.
    private val serializer = Jackson2JsonRedisSerializer(mapper, WeatherBundleDto::class.java)

    private fun sampleBundle() = WeatherBundleDto(
        forecast = OpenMeteoForecastResponse(latitude = 19.07, longitude = 72.87),
        airQuality = null,
        marine = null,
        fetchedAtMs = 1_700_000_000_000L
    )

    // ── Test 1: round-trip ──────────────────────────────────────────────────

    @Test
    fun `serialize then deserialize round-trips WeatherBundleDto correctly`() {
        val original = sampleBundle()
        val bytes = serializer.serialize(original)
        assertNotNull(bytes)

        val recovered = serializer.deserialize(bytes!!)
        assertNotNull(recovered)
        assertEquals(original.fetchedAtMs, recovered!!.fetchedAtMs)
        assertEquals(19.07, recovered.forecast.latitude)
        assertEquals(72.87, recovered.forecast.longitude)
    }

    // ── Test 2: no @class field written ────────────────────────────────────

    @Test
    fun `serialized payload must NOT contain @class type metadata`() {
        val bytes = serializer.serialize(sampleBundle()) ?: error("serialize returned null")
        val json = String(bytes, Charsets.UTF_8)
        assertFalse(json.contains("@class"), "Typed serializer must NOT embed @class: found in: $json")
        assertFalse(json.contains("@type"), "Typed serializer must NOT embed @type: found in: $json")
    }

    // ── Test 3: gadget payload is NOT instantiated ──────────────────────────

    @Test
    fun `payload with at-class gadget field does not instantiate arbitrary class`() {
        // A real gadget-chain attack would embed a fully-qualified classname under "@class".
        // The typed deserializer targets WeatherBundleDto exclusively; it either:
        //   (a) ignores the unknown field (because @JsonIgnoreProperties(ignoreUnknown=true)
        //       is on all nested DTOs), returning a WeatherBundleDto with defaults, OR
        //   (b) raises SerializationException.
        // Either outcome is safe — the class listed under @class is never instantiated.
        val malicious = """
            {
              "@class": "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl",
              "fetchedAtMs": 0,
              "forecast": {}
            }
        """.trimIndent().toByteArray(Charsets.UTF_8)

        // Expect either a safe WeatherBundleDto (unknown fields ignored) or a
        // SerializationException — never a ClassCastException or runtime gadget exec.
        runCatching { serializer.deserialize(malicious) }.fold(
            onSuccess = { result ->
                // Safe outcome: @class field was ignored, we got a WeatherBundleDto back.
                assertNotNull(result, "Expected non-null WeatherBundleDto (gadget field was ignored safely)")
            },
            onFailure = { ex ->
                // Also safe: typed deserializer rejected the foreign payload entirely.
                assert(ex is SerializationException || ex is com.fasterxml.jackson.core.JsonProcessingException) {
                    "Expected SerializationException or JsonProcessingException, got ${ex::class.simpleName}"
                }
            }
        )
    }
}
