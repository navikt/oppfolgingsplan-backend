package no.nav.syfo.cache

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
@ConfigurationProperties(prefix = "valkey")
class ValkeyConfig {
    lateinit var url: URI
    lateinit var username: String
    lateinit var password: String
    var ssl: Boolean = true
}
