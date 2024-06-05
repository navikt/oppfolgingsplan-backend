package no.nav.syfo.brukertilgang

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.oidc.TokenUtil.getIssuerToken
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Service
class BrukertilgangClient(
    private val contextHolder: TokenValidationContextHolder,
    private val metrikk: Metrikk,
    private val tokenDingsConsumer: TokenDingsConsumer,
    @Value("\${syfobrukertilgang.url}") private val baseUrl: String,
    @Value("\${syfobrukertilgang.id}") private var targetApp: String,
) {
    fun hasAccessToAnsatt(ansattFnr: String): Boolean {
        val issuerToken = getIssuerToken(contextHolder, TOKENX)
        val exchangedToken = tokenDingsConsumer.exchangeToken(issuerToken, targetApp)
        val httpEntity = createHttpEntity(exchangedToken, ansattFnr)

        return try {
            val response = getResponse(httpEntity)
            metrikk.countOutgoingReponses(METRIC_CALL_BRUKERTILGANG, response.statusCode.value())
            response.body!!
        } catch (e: RestClientResponseException) {
            handleException(e, httpEntity)
        }
    }

    private fun createHttpEntity(exchangedToken: String, personident: String): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(exchangedToken))
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        headers.add(NAV_CONSUMER_ID_HEADER, APP_CONSUMER_ID)
        headers.add(NAV_PERSONIDENT_HEADER, personident)
        return HttpEntity<Any>(headers)
    }

    private fun getResponse(httpEntity: HttpEntity<*>): ResponseEntity<Boolean> {
        return RestTemplate().exchange(
            "$baseUrl/api/v2/tilgang/ansatt",
            HttpMethod.GET,
            httpEntity,
            Boolean::class.java,
        )
    }

    private fun handleException(e: RestClientResponseException, httpEntity: HttpEntity<*>): Nothing {
        metrikk.countOutgoingReponses(METRIC_CALL_BRUKERTILGANG, e.statusCode.value())
        if (e.statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            throw RequestUnauthorizedException(
                "Unauthorized request to get access to Ansatt from Syfobrukertilgang"
            )
        } else {
            LOG.error(
                "Error requesting ansatt access from syfobrukertilgang with callId {}: ",
                httpEntity.headers[NAV_CALL_ID_HEADER],
                e
            )
            throw e
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BrukertilgangClient::class.java)

        const val METRIC_CALL_BRUKERTILGANG = "call_syfobrukertilgang"
    }
}
