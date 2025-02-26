package no.nav.syfo

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableJwtTokenValidation
class OppfolgingsplanBackendApplication

fun main(args: Array<String>) {
    runApplication<OppfolgingsplanBackendApplication>(args = args)
}
