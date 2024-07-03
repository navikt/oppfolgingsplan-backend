package no.nav.syfo.aareg.exceptions

import org.springframework.web.client.RestClientException

class RestErrorFromAareg(message: String, e: RestClientException) : RuntimeException(message, e)
