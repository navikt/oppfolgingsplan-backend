package no.nav.syfo.brukertilgang

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/brukertilgang"])
class BrukerTilgangController(
    private val contextHolder: TokenValidationContextHolder,
    private val brukertilgangClient: BrukertilgangClient,
    private val brukertilgangService: BrukertilgangService,
    private val metrikk: Metrikk,
    @Value("\${OPPFOLGINGSPLAN_FRONTEND_CLIENT_ID}")
    private val oppfolgingsplanClientId: String,
) {
    @GetMapping
    fun harTilgang(@RequestHeader(NAV_PERSONIDENT_HEADER) fnr: String): RSTilgang {
        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        if (!brukertilgangService.tilgangTilOppslattIdent(innloggetIdent, fnr)) {
            LOG.error("Ikke tilgang: Bruker spør om noen andre enn seg selv eller egne ansatte")
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        metrikk.tellHendelse("sjekk_brukertilgang")
        return RSTilgang(true)
    }

    @GetMapping(path = ["/ansatt"])
    @ResponseBody
    fun accessToAnsatt(@RequestHeader(NAV_PERSONIDENT_HEADER) fnr: String): BrukerTilgang {
        TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)

        metrikk.tellHendelse("accessToIdent")

        return BrukerTilgang(brukertilgangClient.hasAccessToAnsatt(fnr))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BrukerTilgangController::class.java)
    }
}
