package no.nav.syfo.felleskodeverk

import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.cache.ValkeyStore
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class FellesKodeverkClient @Autowired constructor(
    private val metric: Metrikk,
    @Value("\${felleskodeverk.url}") private val url: String,
    @Value("\${felleskodeverk.scope}") private val scope: String,
    private val azureAdTokenClient: AzureAdTokenClient,
    private val valkeyStore: ValkeyStore
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(FellesKodeverkClient::class.java)
        const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
        const val NAV_CONSUMER_ID = "Nav-Consumer-Id"
    }

    fun kodeverkKoderBetydninger(): KodeverkKoderBetydningerResponse {
        val cacheKey = "felleskodeverk_koder_betydninger"
        val cachedValue: KodeverkKoderBetydningerResponse? = valkeyStore.getObject(
            cacheKey,
            KodeverkKoderBetydningerResponse::class.java
        )

        if (cachedValue != null) {
            LOG.info("Using cached value for kodeverk")
            return cachedValue
        }

        val kodeverkYrkerBetydningUrl = "$url/kodeverk/Yrker/koder/betydninger?spraak=nb"
        val accessToken = azureAdTokenClient.getSystemToken(scope)
        return try {
            val response = RestTemplate().exchange(
                kodeverkYrkerBetydningUrl,
                GET,
                entity(accessToken),
                KodeverkKoderBetydningerResponse::class.java
            )
            metric.tellHendelse("call_felleskodeverk_success")
            val responseBody = response.body ?: throw RestClientException("Response body is null")
            valkeyStore.setObject(cacheKey, responseBody, 3600)
            responseBody
        } catch (e: RestClientException) {
            metric.tellHendelse("call_felleskodeverk_fail")
            LOG.error("Error from Felles Kodeverk with request-url: $kodeverkYrkerBetydningUrl", e)
            throw RestClientException("Tried to get kodeBetydninger from Felles Kodeverk", e)
        }
    }

    private fun entity(token: String): HttpEntity<Void> {
        val headers = HttpHeaders().apply {
            add(NAV_CALL_ID_HEADER, createCallId())
            add(NAV_CONSUMER_ID, APP_CONSUMER_ID)
            add(HttpHeaders.AUTHORIZATION, bearerHeader(token))
        }
        return HttpEntity(headers)
    }
}
