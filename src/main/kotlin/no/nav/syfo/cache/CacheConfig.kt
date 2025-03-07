package no.nav.syfo.cache

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

@Configuration
class CacheConfig(private val valkeyConfig: ValkeyConfig) {

    @Bean
    @Primary
    fun jedisPool(): JedisPool {
        val hostAndPort = HostAndPort(valkeyConfig.url.host, valkeyConfig.url.port)
        val clientConfig = DefaultJedisClientConfig.builder()
            .ssl(valkeyConfig.ssl)
            .user(valkeyConfig.username)
            .password(valkeyConfig.password)
            .build()
        return JedisPool(JedisPoolConfig(), hostAndPort, clientConfig)
    }

    @Bean
    fun valkeyStore(jedisPool: JedisPool): ValkeyStore {
        return ValkeyStore(jedisPool)
    }
}
