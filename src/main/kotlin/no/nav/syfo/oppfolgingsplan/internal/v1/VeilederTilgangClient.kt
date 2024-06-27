package no.nav.syfo.oppfolgingsplan.internal.v1

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.auth.oidc.OIDCIssuer
import no.nav.syfo.auth.oidc.TokenUtil.getIssuerToken
import no.nav.syfo.domain.Fodselsnummer
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.APP_CONSUMER_ID
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

@Service
class VeilederTilgangClient(
    @Value("\${istilgangskontroll.client.id}") private val istilgangskontrollClientId: String,
    @Value("\${istilgangskontroll.url}") private val istilgangskontrollUrl: String,
    private val azureAdTokenClient: AzureAdTokenClient,
    private val metric: Metrikk,
    private val contextHolder: TokenValidationContextHolder,
) {
    private val tilgangskontrollPersonUrl = "$istilgangskontrollUrl$TILGANGSKONTROLL_PERSON_PATH"
    private val tilgangskontrollPersonSYFOUrl = "$istilgangskontrollUrl$TILGANGSKONTROLL_SYFO_PATH"

    fun throwExceptionIfVeilederWithoutAccessWithOBO(fnr: Fodselsnummer) {
        val harTilgang = hasVeilederAccessToPersonWithOBO(fnr)
        if (!harTilgang) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied")
        }
    }

    private fun hasVeilederAccessToPersonWithOBO(fnr: Fodselsnummer): Boolean {
        val oboToken = azureAdTokenClient.getOnBehalfOfToken(
            scopeClientId = istilgangskontrollClientId,
            token = getIssuerToken(contextHolder, OIDCIssuer.INTERN_AZUREAD_V2),
        )
        val httpEntity = entityPerson(
            personIdentNumber = fnr,
            token = oboToken,
        )
        return checkAccess(
            httpEntity = httpEntity,
            url = tilgangskontrollPersonUrl,
        )
    }

    fun throwExceptionIfVeilederWithoutAccessToSYFOWithOBO() {
        val harTilgang = hasVeilederAccessToSYFOWithOBO()
        if (!harTilgang) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied")
        }
    }

    private fun hasVeilederAccessToSYFOWithOBO(): Boolean {
        val oboToken = azureAdTokenClient.getOnBehalfOfToken(
            scopeClientId = istilgangskontrollClientId,
            token = getIssuerToken(contextHolder, OIDCIssuer.INTERN_AZUREAD_V2),
        )
        val httpEntity = entitySYFO(token = oboToken)
        return checkAccess(
            httpEntity = httpEntity,
            url = tilgangskontrollPersonSYFOUrl,
        )
    }

    private fun entityPerson(
        personIdentNumber: Fodselsnummer,
        token: String,
    ): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.setBearerAuth(token)
        headers[NAV_PERSONIDENT_HEADER] = personIdentNumber.value
        headers[NAV_CALL_ID_HEADER] = createCallId()
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        return HttpEntity(headers)
    }

    private fun entitySYFO(token: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.setBearerAuth(token)
        headers[NAV_CALL_ID_HEADER] = createCallId()
        headers[NAV_CONSUMER_ID_HEADER] = APP_CONSUMER_ID
        return HttpEntity(headers)
    }

    private fun checkAccess(
        httpEntity: HttpEntity<String>,
        url: String,
    ): Boolean {
        return try {
            val tilgang = RestTemplate().exchange(
                url,
                HttpMethod.GET,
                httpEntity,
                Tilgang::class.java,
            )
            tilgang.body!!.erGodkjent
        } catch (e: HttpClientErrorException) {
            if (e.statusCode.value() == HttpStatus.FORBIDDEN.value()) {
                false
            } else {
                metric.tellHendelse(METRIC_CALL_VEILEDERTILGANG_USER_FAIL)
                LOG.error(
                    "Error requesting ansatt access from istilgangskontroll with status-${e.statusCode.value()} " +
                        "callId-${httpEntity.headers[NAV_CALL_ID_HEADER]}: ",
                    e
                )
                throw e
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(VeilederTilgangClient::class.java)

        private const val METRIC_CALL_VEILEDERTILGANG_BASE = "call_istilgangskontroll"
        private const val METRIC_CALL_VEILEDERTILGANG_USER_FAIL = "${METRIC_CALL_VEILEDERTILGANG_BASE}_user_fail"

        const val TILGANGSKONTROLL_COMMON_PATH = "/api/tilgang/navident"
        const val TILGANGSKONTROLL_PERSON_PATH = "$TILGANGSKONTROLL_COMMON_PATH/person"
        const val TILGANGSKONTROLL_SYFO_PATH = "$TILGANGSKONTROLL_COMMON_PATH/syfo"
    }

    data class Tilgang(
        val erGodkjent: Boolean,
    )
}
