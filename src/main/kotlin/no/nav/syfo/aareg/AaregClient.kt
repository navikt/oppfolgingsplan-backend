package no.nav.syfo.aareg

import no.nav.syfo.aareg.exceptions.RestErrorFromAareg
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.sts.StsClient
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.GET
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class AaregClient(
    private val metrikk: Metrikk,
    private val stsClient: StsClient,
    @Value("\${aareg.services.url}") private val url: String
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AaregClient::class.java)
        const val NAV_CONSUMER_TOKEN_HEADER = "Nav-Consumer-Token"
        const val NAV_PERSONIDENT_HEADER = "Nav-Personident"
    }

    @Cacheable(cacheNames = ["arbeidsforholdAT"], key = "#fnr", condition = "#fnr != null")
    fun arbeidsforholdArbeidstaker(fnr: String): List<Arbeidsforhold> {
        metrikk.tellHendelse("call_aareg")
        val token = stsClient.token()

        return try {
            val response: ResponseEntity<List<Arbeidsforhold>> = RestTemplate().exchange(
                arbeidstakerUrl(),
                GET,
                entity(fnr, token),
                object : ParameterizedTypeReference<List<Arbeidsforhold>>() {}
            )
            metrikk.tellHendelse("call_aareg_success")
            response.body ?: emptyList()
        } catch (e: RestClientException) {
            metrikk.tellHendelse("call_aareg_fail")
            LOG.error("Error from AAREG with request-url: $url", e)
            throw RestErrorFromAareg("Tried to get arbeidsforhold for arbeidstaker", e)
        }
    }

    private fun entity(fnr: String, token: String): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(token))
        headers.add(NAV_CONSUMER_TOKEN_HEADER, bearerHeader(token))
        headers.add(NAV_PERSONIDENT_HEADER, fnr)
        return HttpEntity<Any>(headers)
    }

    private fun arbeidstakerUrl(): String {
        return "$url/v1/arbeidstaker/arbeidsforhold"
    }
}
