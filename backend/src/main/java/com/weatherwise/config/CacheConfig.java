package com.weatherwise.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory
    ) {

        RedisCacheConfiguration defaultConfiguration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .computePrefixWith(cacheName -> "roamora:v3:" + cacheName + "::")
                        .disableCachingNullValues()
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(
                                                RedisSerializer.json()
                                        )
                        );

        return RedisCacheManager.builder(connectionFactory)

                .cacheDefaults(
                        defaultConfiguration
                                .entryTtl(Duration.ofMinutes(10))
                )

                .withCacheConfiguration(
                        "weather",
                        defaultConfiguration
                                .entryTtl(Duration.ofMinutes(10))
                )

                .withCacheConfiguration(
                        "routePlans",
                        defaultConfiguration
                                .entryTtl(Duration.ofMinutes(5))
                )

                .withCacheConfiguration(
                        "nearbyPlaces",
                        defaultConfiguration
                                .entryTtl(Duration.ofMinutes(2))
                )

                .build();
    }
}