package no.nav.syfo

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import no.nav.syfo.util.PropertyUtil
import no.nav.syfo.util.ToggleUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import java.util.Objects

@Configuration
@Import(MockOAuth2ServerAutoConfiguration::class)
class LocalApplicationConfig(environment: Environment) {
    init {
        System.setProperty("SECURITYTOKENSERVICE_URL", Objects.requireNonNull(environment.getProperty("securitytokenservice.url")))
        System.setProperty("SRV_USERNAME", Objects.requireNonNull(environment.getProperty("srvsyfooppfolgingsplanservice.username")))
        System.setProperty("SRV_PASSWORD", Objects.requireNonNull(environment.getProperty("srvsyfooppfolgingsplanservice.password")))
        System.setProperty(PropertyUtil.FASIT_ENVIRONMENT_NAME, ToggleUtil.ENVIRONMENT_MODE.q1.name)
        System.setProperty(PropertyUtil.LOCAL_MOCK, "true")
    }

    @Bean
    fun mockOAuthServer(@Value("\${mock.token.server.port}") mockTokenServerPort: Int): MockOAuth2Server {
        var server = MockOAuth2Server()
        server.start(mockTokenServerPort)
        return server
    }
}
