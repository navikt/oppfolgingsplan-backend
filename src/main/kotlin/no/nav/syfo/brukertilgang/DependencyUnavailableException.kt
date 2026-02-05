package no.nav.syfo.brukertilgang

import no.nav.syfo.exception.AbstractApiError
import no.nav.syfo.exception.LogLevel
import org.springframework.http.HttpStatus

class DependencyUnavailableException(message: String, cause: Throwable? = null) : AbstractApiError(
    message = message,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    reason = HttpStatus.SERVICE_UNAVAILABLE.reasonPhrase,
    loglevel = LogLevel.WARN,
    grunn = cause
)
