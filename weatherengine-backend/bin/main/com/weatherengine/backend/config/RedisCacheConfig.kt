package com.weatherengine.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.Cache
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class RedisCacheConfig : CachingConfigurer {

    private val logger = LoggerFactory.getLogger(RedisCacheConfig::class.java)

    @Bean
    @ConditionalOnProperty(name = ["spring.cache.type"], havingValue = "redis", matchIfMissing = true)
    fun redisCacheConfiguration(): RedisCacheConfiguration {
        val mapper = ObjectMapper().registerKotlinModule()
        mapper.activateDefaultTyping(
            mapper.polymorphicTypeValidator,
            ObjectMapper.DefaultTyping.NON_FINAL
        )
        val serializer = GenericJackson2JsonRedisSerializer(mapper)

        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )
    }

    override fun errorHandler(): CacheErrorHandler {
        return object : CacheErrorHandler {
            override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) {
                logger.warn("Redis cache GET failed for key {}: {}", key, exception.message)
            }

            override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) {
                logger.warn("Redis cache PUT failed for key {}: {}", key, exception.message)
            }

            override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) {
                logger.warn("Redis cache EVICT failed for key {}: {}", key, exception.message)
            }

            override fun handleCacheClearError(exception: RuntimeException, cache: Cache) {
                logger.warn("Redis cache CLEAR failed: {}", exception.message)
            }
        }
    }
}
