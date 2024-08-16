package no.nav.syfo.felleskodeverk

import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class FellesKodeverkClient @Autowired constructor(
    private val metric: Metrikk,
    @Value("\${felleskodeverk.url}") private val url: String
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(FellesKodeverkClient::class.java)
        const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
        const val NAV_CONSUMER_ID = "Nav-Consumer-Id"
    }

    @Cacheable(cacheNames = ["cachenameFelleskodeverkBetydninger"])
    fun kodeverkKoderBetydninger(): KodeverkKoderBetydningerResponse {
        val kodeverkYrkerBetydningUrl = "$url/kodeverk/Yrker/koder/betydninger?spraak=nb"
        return try {
            val response = RestTemplate().exchange(
                kodeverkYrkerBetydningUrl,
                GET,
                entity(),
                KodeverkKoderBetydningerResponse::class.java
            )
            metric.tellHendelse("call_felleskodeverk_success")
            response.body ?: throw RestClientException("Response body is null")
        } catch (e: RestClientException) {
            metric.tellHendelse("call_felleskodeverk_fail")
            LOG.error("Error from Felles Kodeverk with request-url: $kodeverkYrkerBetydningUrl", e)
            throw RestClientException("Tried to get kodeBetydninger from Felles Kodeverk", e)
        }
    }

    private fun entity(): HttpEntity<Void> {
        val headers = HttpHeaders().apply {
            add(NAV_CALL_ID_HEADER, createCallId())
            add(NAV_CONSUMER_ID, APP_CONSUMER_ID)
        }
        return HttpEntity(headers)
    }
}