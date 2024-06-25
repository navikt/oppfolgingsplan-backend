package no.nav.syfo.ereg

import javax.inject.Inject
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Service
class EregClient @Inject constructor(
    @Value("\${ereg.url}") private val baseUrl: String,
    private val metric: Metrikk,
) {
    fun eregResponse(virksomhetsnummer: String): EregOrganisasjonResponse {
        try {
            val response = RestTemplate().exchange(
                getEregUrl(),
                HttpMethod.GET,
                entity(),
                EregOrganisasjonResponse::class.java,
                virksomhetsnummer,
            )
            val eregResponse = response.body!!
            metric.tellHendelse(METRIC_CALL_EREG_SUCCESS)
            return eregResponse
        } catch (e: RestClientResponseException) {
            metric.tellHendelse(METRIC_CALL_EREG_FAIL)
            val message =
                "Call to get name Virksomhetsnummer from EREG failed with status:" +
                        " ${e.statusCode.value()} and message: ${e.responseBodyAsString}"
            LOG.error(message)
            throw e
        }
    }

    fun virksomhetsnavn(virksomhetsnummer: String): String {
        return eregResponse(virksomhetsnummer).navn()
    }

    private fun getEregUrl(): String {
        return "$baseUrl/ereg/api/v2/organisasjon/{virksomhetsnummer}"
    }

    private fun entity(): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        headers[NAV_CALL_ID_HEADER] = createCallId()
        return HttpEntity(headers)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(EregClient::class.java)

        private const val METRIC_CALL_EREG_SUCCESS = "call_ereg_success"
        private const val METRIC_CALL_EREG_FAIL = "call_ereg_fail"
    }
}
