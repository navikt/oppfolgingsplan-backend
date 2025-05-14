package no.nav.syfo.kontaktinfo

import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.NAV_CALL_ID_HEADER
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

// import no.nav.syfo.cache.ValkeyStore
@Service
class KrrClient @Autowired constructor(
    private val azureAdTokenConsumer: AzureAdTokenClient,
    private val metric: Metrikk,
    @Value("\${krr.scope}") private val krrScope: String,
    @Value("\${krr.url}") val krrUrl: String,
//    private val valkeyStore: ValkeyStore
) {
    fun kontaktinformasjon(fnr: String): DigitalKontaktinfo {
//        val cacheKey = "krr_fnr_$fnr"
//        val cachedValue: DigitalKontaktinfo? = valkeyStore.getObject(cacheKey, DigitalKontaktinfo::class.java)

//        if (cachedValue != null) {
//            return cachedValue
//        }

        log.info("Henter kontaktinformasjon fra KRR")
        val accessToken = "Bearer ${azureAdTokenConsumer.getSystemToken(krrScope)}"
        val response = RestTemplate().exchange(
            krrUrl,
            HttpMethod.POST,
            entity(fnr, accessToken),
            PostPersonerResponse::class.java
        )

        if (response.statusCode != HttpStatus.OK) {
            logAndThrowError(response, "Received response with status code: ${response.statusCode}")
        }
        log.info("kontaktinfo fra krr ${response.body}")
        val kontaktinfo = response.body?.let {
            metric.countOutgoingReponses(METRIC_CALL_KRR, response.statusCode.value())
            it.personer.getOrDefault(fnr, null)
                ?: logAndThrowError(response, "Response did not contain person")
        } ?: logAndThrowError(response, "ResponseBody is null")

//        valkeyStore.setObject(cacheKey, kontaktinfo, 3600)
        log.info("Decoded kotaktinfo $kontaktinfo")
        return kontaktinfo
    }

    private fun logAndThrowError(response: ResponseEntity<PostPersonerResponse>, message: String): Nothing {
        log.error(message)
        metric.countOutgoingReponses(METRIC_CALL_KRR, response.statusCode.value())
        throw KrrRequestFailedException(message)
    }

    private fun entity(fnr: String, accessToken: String): HttpEntity<PostPersonerRequest> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers[HttpHeaders.AUTHORIZATION] = accessToken
        headers[NAV_CALL_ID_HEADER] = createCallId()
        return HttpEntity(PostPersonerRequest(setOf(fnr)), headers)
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
