package no.nav.syfo.kontaktinfo

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
class KrrRequestFailedException(
    message: String = ""
) : RuntimeException("Request to get Kontaktinformasjon from KRR Failed: $message")
