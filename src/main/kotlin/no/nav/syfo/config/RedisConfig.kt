package no.nav.syfo.config

import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import java.net.URI

@Configuration
class RedisConfig {
    @Bean
    fun redisConnectionFactory(
        @Value("\${spring.data.redis.url}") url: URI,
        @Value("\${spring.data.redis.password}") password: String,
        @Value("\${spring.data.redis.username}") username: String,
    ): LettuceConnectionFactory {
        val redisURI = RedisURI.create(url)
        return LettuceConnectionFactory(
            RedisStandaloneConfiguration(url.host, url.port).apply {
                setUsername(username)
                setPassword(password)
                database = redisURI.database
            },
            LettuceClientConfiguration.builder().apply {
                if (redisURI.isSsl) {
                    useSsl()
                }
            }.build()
        )
    }
}
