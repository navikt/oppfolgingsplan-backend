package no.nav.syfo.narmesteleder

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.oidc.TokenUtil
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class NarmesteLederClient(
    @Value("\${narmesteleder.url}") private val baseUrl: String,
    @Value("\${narmesteleder.client.id}") private var targetApp: String,
    private val tokenDingsConsumer: TokenDingsConsumer,
    private val contextHolder: TokenValidationContextHolder,
) {
    @Cacheable(value = ["aktive_ledere"], key = "#ansattFnr", condition = "#ansattFnr != null")
    fun alleLedereForSykmeldt(
        ansattFnr: String,
    ): List<NarmesteLederRelasjonDTO> {

        val issuerToken = TokenUtil.getIssuerToken(contextHolder, TokenXUtil.TokenXIssuer.TOKENX)
        val exchangedToken = tokenDingsConsumer.exchangeToken(issuerToken, targetApp)

        try {
            val response = getResponse(
                fnr = ansattFnr,
                accessToken = exchangedToken
            )

            val relasjoner = response.body ?: emptyArray()

            return relasjoner
                .filter { it.arbeidstakerPersonIdentNumber == ansattFnr }
        } catch (e: RestClientResponseException) {
            log.error(
                "Error while requesting all NarmesteLeder of sykmeldt. Stacktrace: {}",
                e.stackTraceToString()
            )
            return emptyList()
        }
    }

    @Cacheable(value = ["aktive_ansatte"], key = "{#ansattFnr, #virksomhetsnummer}", condition = "{#ansattFnr != null, #virksomhetsnummer != null}")
    fun aktivNarmesteLederIVirksomhet(
        ansattFnr: String,
        virksomhetsnummer: String,
    ): NarmesteLederRelasjonDTO? {
        try {
            val narmesteLederRelasjoner = alleLedereForSykmeldt(ansattFnr)

            return narmesteLederRelasjoner
                .filter { it.aktivTom == null }
                .firstOrNull { it.virksomhetsnummer == virksomhetsnummer }
        } catch (e: RestClientResponseException) {
            log.error(
                "Error while requesting aktive leder in virksomhet. Stacktrace: {}",
                e.stackTraceToString()
            )
            return null
        }
    }

    private fun headers(fnr: String, accessToken: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers[HttpHeaders.AUTHORIZATION] = bearerHeader(accessToken)
        headers[NAV_PERSONIDENT_HEADER] = fnr
        headers[NAV_CALL_ID_HEADER] = createCallId()
        return HttpEntity(headers)
    }

    private fun getResponse(
        fnr: String,
        accessToken: String
    ): ResponseEntity<Array<NarmesteLederRelasjonDTO>> {
        return RestTemplate().exchange(
            "$baseUrl/api/selvbetjening/v1/narmestelederrelasjoner",
            HttpMethod.GET,
            headers(fnr, accessToken),
            Array<NarmesteLederRelasjonDTO>::class.java,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(NarmesteLederClient::class.java)

        private fun createCallId(): String {
            val randomUUID = UUID.randomUUID().toString()
            return "oppfolgingsplan-backend-$randomUUID"
        }
    }
}
