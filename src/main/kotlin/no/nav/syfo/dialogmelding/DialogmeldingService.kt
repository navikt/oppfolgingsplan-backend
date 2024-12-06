package no.nav.syfo.dialogmelding

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.createCallId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import no.nav.syfo.auth.oidc.TokenUtil.getIssuerToken
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX

@Service
class DialogmeldingService(
    @Value("\${isdialogmelding.url}") dialogmeldingUrl: String,
    @Value("\${isdialogmelding.client.id}")
    private val dialogmeldingClientId: String,
    private val metrikk: Metrikk,
    private val contextHolder: TokenValidationContextHolder,
    private val tokenDingsConsumer: TokenDingsConsumer,
) {

    private val delMedFastlegeUriTemplate: UriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(dialogmeldingUrl)
        .path(SEND_OPPFOLGINGSPLAN_PATH)
    private val log = LoggerFactory.getLogger(DialogmeldingService::class.java)

    fun sendOppfolgingsplanTilFastlege(sykmeldtFnr: String, pdf: ByteArray) {
        val rsOppfoelgingsplan = RSOppfoelgingsplan(sykmeldtFnr, pdf)
        val delMedFastlegeUri = delMedFastlegeUriTemplate.build().toUri()
        val token = getIssuerToken(contextHolder, TOKENX)
        val exchangedToken = tokenDingsConsumer.exchangeToken(token, dialogmeldingClientId)
        try {
            kallUriMedTemplate(
                delMedFastlegeUri,
                rsOppfoelgingsplan,
                exchangedToken
            )
        } catch (e: OppslagFeiletException) {
            log.warn("Fanget OppslagFeiletException: {}", e.message)
            throw e
        }
    }

    private fun kallUriMedTemplate(uri: URI, rsOppfoelgingsplan: RSOppfoelgingsplan, token: String) {
        try {
            RestTemplate().postForLocation(uri, entity(rsOppfoelgingsplan, token))
            tellPlanDeltMedFastlegeKall(true)
        } catch (e: HttpClientErrorException) {
            val responsekode = e.statusCode.value()
            if (responsekode == 404) {
                throw OppslagFeiletException("Feil ved oppslag av fastlege eller partnerinformasjon")
            } else {
                log.error("Feil ved sending av oppfølgingsdialog til fastlege Fikk responskode $responsekode", e)
            }
            throw e
        } catch (e: HttpServerErrorException) {
            val responsekode = e.statusCode.value()
            log.error("Feil ved sending av oppfølgingsdialog til fastlege: Fikk responskode $responsekode", e)
            throw InnsendingFeiletException("Kunne ikke dele med fastlege")
        } catch (e: Exception) {
            log.error("Feil ved sending av oppfølgingsdialog til fastlege", e)
            throw e
        }
    }

    private fun entity(rsOppfoelgingsplan: RSOppfoelgingsplan, token: String): HttpEntity<RSOppfoelgingsplan> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.add(AUTHORIZATION, bearerHeader(token))
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        return HttpEntity(rsOppfoelgingsplan, headers)
    }

    private fun tellPlanDeltMedFastlegeKall(delt: Boolean) {
        metrikk.tellHendelse("plan_delt_med_fastlege")
    }

    companion object {
        const val SEND_OPPFOLGINGSPLAN_PATH = "/api/person/v1/oppfolgingsplan"
    }
}
