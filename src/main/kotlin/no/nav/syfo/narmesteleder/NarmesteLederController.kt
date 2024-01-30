package no.nav.syfo.narmesteleder

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.brukertilgang.BrukertilgangService
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.ORGNUMMER_HEADER
import no.nav.syfo.util.fodselsnummerInvalid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/narmesteleder"])
class NarmesteLederController @Autowired constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metrikk: Metrikk,
    private val narmesteLederClient: NarmesteLederClient,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
    private val brukertilgangService: BrukertilgangService,
) {

    @ResponseBody
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE], path = ["/virksomhet"])
    fun getAktivNarmesteLederIVirksomhet(
        @RequestHeader(NAV_PERSONIDENT_HEADER) fnr: String,
        @RequestHeader(ORGNUMMER_HEADER) virksomhetsnummer: String,
    ): ResponseEntity<NarmesteLeder?> {
        metrikk.tellHendelse("get_narmesteledere")

        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value

        return if (fodselsnummerInvalid(fnr)) {
            LOG.error("Ugyldig fnr ved henting av nærmeste ledere")
            ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build()
        } else {
            if (!brukertilgangService.tilgangTilOppslattIdent(innloggetIdent, fnr)) {
                LOG.error("Ikke tilgang til nærmeste ledere: Bruker spør om noen andre enn seg selv eller egne ansatte")
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build()
            } else {
                val narmesteLedere = narmesteLederClient.aktivNarmesteLederIVirksomhet(
                    ansattFnr = fnr,
                    narmesteLederIdent = innloggetIdent,
                    virksomhetsnummer = virksomhetsnummer
                )
                return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .build()
            }
        }
    }

    @ResponseBody
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE], path = ["/alle"])
    fun getNarmesteLedere(): ResponseEntity<List<NarmesteLeder>> {
        metrikk.tellHendelse("get_narmesteledere")

        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value

        val narmesteLedere = narmesteLederClient.alleAktiveLedereForSykmeldt(ansattFnr = innloggetIdent)

       return ResponseEntity
           .status(HttpStatus.NO_CONTENT)
           .build()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(NarmesteLederController::class.java)
    }
}
