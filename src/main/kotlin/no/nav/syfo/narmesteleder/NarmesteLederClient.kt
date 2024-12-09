package no.nav.syfo.narmesteleder

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.oidc.TokenUtil
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
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
            val response = getNarmestelederRelasjoner(
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

    @Cacheable(
        value = ["aktiv_narmeste_leder"],
        key = "{#ansattFnr, #virksomhetsnummer}",
        condition = "{#ansattFnr != null, #virksomhetsnummer != null}"
    )
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

    @Cacheable(value = ["narmesteleder_ansatte"], key = "#fnr", condition = "#fnr != null")
    fun ansatte(fnr: String): List<Ansatt>? {
        val issuerToken = TokenUtil.getIssuerToken(contextHolder, TokenXUtil.TokenXIssuer.TOKENX)
        val exchangedToken = tokenDingsConsumer.exchangeToken(issuerToken, targetApp)

        val response: ResponseEntity<Array<NarmesteLederRelasjonDTO>> = getAktiveAnsatte(fnr, exchangedToken)
        return if (response.statusCode.is2xxSuccessful) {
            response.body?.map { Ansatt(it.arbeidstakerPersonIdentNumber, it.virksomhetsnummer) }
        } else {
            throw NarmesteLederException("Error fetching ansatte: ${response.statusCode}")
        }
    }

    @Cacheable(
        value = ["erNaermesteLederForAnsatt"],
        key = "{#naermesteLederFnr, #ansattFnr}",
        condition = "#naermesteLederFnr != null && #ansattFnr != null"
    )
    fun erNaermesteLederForAnsatt(naermesteLederFnr: String, ansattFnr: String): Boolean {
        val ansatteFnr = ansatte(naermesteLederFnr)?.map { it.fnr } ?: emptyList()
        return ansatteFnr.contains(ansattFnr)
    }

    private fun headersForSykmeldt(fnr: String, accessToken: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers[HttpHeaders.AUTHORIZATION] = bearerHeader(accessToken)
        headers[NAV_PERSONIDENT_HEADER] = fnr
        headers[NAV_CALL_ID_HEADER] = createCallId()
        return HttpEntity(headers)
    }

    private fun headersForNarmesteLeder(fnr: String, accessToken: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers[HttpHeaders.AUTHORIZATION] = bearerHeader(accessToken)
        headers.add("Narmeste-Leder-Fnr", fnr)
        headers[NAV_CALL_ID_HEADER] = createCallId()
        return HttpEntity(headers)
    }

    private fun getNarmestelederRelasjoner(
        fnr: String,
        accessToken: String
    ): ResponseEntity<Array<NarmesteLederRelasjonDTO>> {
        return RestTemplate().exchange(
            "$baseUrl/api/selvbetjening/v1/narmestelederrelasjoner",
            HttpMethod.GET,
            headersForSykmeldt(fnr, accessToken),
            Array<NarmesteLederRelasjonDTO>::class.java,
        )
    }

    private fun getAktiveAnsatte(
        fnr: String,
        accessToken: String
    ): ResponseEntity<Array<NarmesteLederRelasjonDTO>> {
        return RestTemplate().exchange(
            "$baseUrl/leder/narmesteleder/aktive",
            HttpMethod.GET,
            headersForNarmesteLeder(fnr, accessToken),
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

data class Ansatt(
    var fnr: String?,
    var virksomhetsnummer: String?
)

class NarmesteLederException(message: String) : RuntimeException(message)
