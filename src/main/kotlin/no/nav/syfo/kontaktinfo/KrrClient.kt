package no.nav.syfo.kontaktinfo

import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class KrrClient @Autowired constructor(
    private val azureAdTokenConsumer: AzureAdTokenClient,
    private val metric: Metrikk,
    @Value("\${krr.scope}") private val krrScope: String,
    @Value("\${krr.url}") val krrUrl: String,
) {
    fun kontaktinformasjon(fnr: String): DigitalKontaktinfo {
        val accessToken = "Bearer ${azureAdTokenConsumer.getSystemToken(krrScope)}"
        val response = RestTemplate().exchange(
            krrUrl,
            HttpMethod.GET,
            entity(fnr, accessToken),
            String::class.java
        )

        if (response.statusCode != HttpStatus.OK) {
            logAndThrowError(response, "Received response with status code: ${response.statusCode}")
        }

        return response.body?.let {
            metric.countOutgoingReponses(METRIC_CALL_KRR, response.statusCode.value())
            KontaktinfoMapper.mapPerson(it)
        } ?: logAndThrowError(response, "ResponseBody is null")
    }

    private fun logAndThrowError(response: ResponseEntity<String>, message: String): Nothing {
        log.error(message)
        metric.countOutgoingReponses(METRIC_CALL_KRR, response.statusCode.value())
        throw KrrRequestFailedException(message)
    }

    private fun entity(fnr: String, accessToken: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers[HttpHeaders.AUTHORIZATION] = accessToken
        headers[NAV_PERSONIDENT_HEADER] = fnr
        headers[NAV_CALL_ID_HEADER] = createCallId()
        return HttpEntity(headers)
    }

    companion object {
        private val log = LoggerFactory.getLogger(KrrClient::class.java)
        const val METRIC_CALL_KRR = "call_krr"

        private fun createCallId(): String {
            val randomUUID = UUID.randomUUID().toString()
            return "oppfolgingsplan-backend-$randomUUID"
        }
    }
}
