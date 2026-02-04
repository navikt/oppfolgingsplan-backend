package no.nav.syfo.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.util.function.Supplier

@Configuration
class RestTemplateConfig {
    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .requestFactory(
                Supplier {
                    SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(2_000)
                        setReadTimeout(5_000)
                    }
                }
            )
            .build()
    }
}
