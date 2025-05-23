package no.nav.syfo.aareg

import no.nav.syfo.aareg.exceptions.RestErrorFromAareg
import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.cache.ValkeyStore
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    private val azureAdTokenClient: AzureAdTokenClient,
    private val valkeyStore: ValkeyStore,
    @Value("\${aareg.services.url}") private val url: String,
    @Value("\${aareg.scope}") private val scope: String
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AaregClient::class.java)
        const val NAV_PERSONIDENT_HEADER = "Nav-Personident"
    }

    fun arbeidsforholdArbeidstaker(fnr: String): List<Arbeidsforhold> {
        val cacheKey = "aareg_arbeidsforholdAT_$fnr"
        val cachedValue: List<Arbeidsforhold>? = valkeyStore.getListObject(cacheKey, Arbeidsforhold::class.java)

        if (cachedValue != null) {
            LOG.info("Using cached value for arbeidsforhold")
            return cachedValue
        }

        LOG.info("Henter arbeidsforhold for arbeidstaker")
        metrikk.tellHendelse("call_aareg")
        val token = azureAdTokenClient.getSystemToken(scope)

        return try {
            val response: ResponseEntity<List<Arbeidsforhold>> = RestTemplate().exchange(
                arbeidstakerUrl,
                GET,
                entity(fnr, token),
                object : ParameterizedTypeReference<List<Arbeidsforhold>>() {}
            )
            metrikk.tellHendelse("call_aareg_success")
            val arbeidsforhold = response.body ?: emptyList()
            valkeyStore.setObject(cacheKey, arbeidsforhold, 3600)
            arbeidsforhold
        } catch (e: RestClientException) {
            metrikk.tellHendelse("call_aareg_fail")
            LOG.error("Error from AAREG with request-url: $url", e)
            throw RestErrorFromAareg("Tried to get arbeidsforhold for arbeidstaker", e)
        }
    }

    private fun entity(fnr: String, token: String): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(token))
        headers.add(NAV_PERSONIDENT_HEADER, fnr)
        return HttpEntity<Any>(headers)
    }

    val arbeidstakerUrl: String = "$url/api/v1/arbeidstaker/arbeidsforhold"
}
