package com.weatherengine.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.weatherengine.backend.weather.WeatherBundleDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.Cache
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.cache.interceptor.CacheErrorHandler
import java.time.Duration

/** Cache name — bump version whenever the cached type shape changes. */
const val WEATHER_CACHE_NAME = "weather-bundle-v4"

/**
 * Redis cache configuration.
 *
 * Uses a TYPED serializer (Jackson2JsonRedisSerializer<WeatherBundleDto>) so that
 * no polymorphic "@class" field is ever written to or read from Redis.
 * This eliminates the Jackson default-typing gadget-chain attack surface.
 *
 * NOTE: Only WeatherBundleDto is cached. If a second type needs caching, add a
 * separate per-cache config entry in [redisCacheManager] rather than falling back
 * to GenericJackson2JsonRedisSerializer + defaultTyping.
 */
@Configuration
class RedisCacheConfig(
    @Value("\${spring.cache.redis.time-to-live:900000}") private val ttlMs: Long = 900_000L
) : CachingConfigurer {

    private val logger = LoggerFactory.getLogger(RedisCacheConfig::class.java)

    /** Typed serializer — no @class field, no defaultTyping. */
    private fun weatherBundleSerializer(): Jackson2JsonRedisSerializer<WeatherBundleDto> {
        val mapper = ObjectMapper().registerKotlinModule()
        // Do NOT call activateDefaultTyping here — typed serializer does not need it.
        return Jackson2JsonRedisSerializer(mapper, WeatherBundleDto::class.java)
    }

    private fun weatherBundleCacheConfig(): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMillis(ttlMs))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(weatherBundleSerializer())
            )

    @Bean
    @ConditionalOnProperty(name = ["spring.cache.type"], havingValue = "redis", matchIfMissing = true)
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager =
        RedisCacheManager.builder(connectionFactory)
            .withCacheConfiguration(WEATHER_CACHE_NAME, weatherBundleCacheConfig())
            // Each new cached type gets its own entry here — never a shared wildcard config.
            .build()

    override fun errorHandler(): CacheErrorHandler = object : CacheErrorHandler {
        override fun handleCacheGetError(e: RuntimeException, c: Cache, k: Any) =
            logger.warn("Redis GET failed ({}): {}", k, e.message)
        override fun handleCachePutError(e: RuntimeException, c: Cache, k: Any, v: Any?) =
            logger.warn("Redis PUT failed ({}): {}", k, e.message)
        override fun handleCacheEvictError(e: RuntimeException, c: Cache, k: Any) =
            logger.warn("Redis EVICT failed ({}): {}", k, e.message)
        override fun handleCacheClearError(e: RuntimeException, c: Cache) =
            logger.warn("Redis CLEAR failed: {}", e.message)
    }
}
