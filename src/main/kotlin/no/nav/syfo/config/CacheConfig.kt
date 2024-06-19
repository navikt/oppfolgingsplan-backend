package no.nav.syfo.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration

@Configuration
@EnableCaching
@Profile("remote")

class CacheConfig {
       companion object {
           const val CACHENAME_EREG_VIRKSOMHETSNAVN: String = "virksomhetsnavn"
       }

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        // create a map of cache names and their configurations
        val cacheConfiguration: MutableMap<String, RedisCacheConfiguration> = HashMap()


        var defaultConfig: RedisCacheConfiguration = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(1L))
        // add CACHENAME_EREG_VIRKSOMHETSNAVN to redisCacheConfiguration
        cacheConfiguration[CACHENAME_EREG_VIRKSOMHETSNAVN] = defaultConfig

        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfiguration)
            .build()
        /*return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
            .cacheDefaults(redisCacheConfiguration())
            .build()*/
    }
}