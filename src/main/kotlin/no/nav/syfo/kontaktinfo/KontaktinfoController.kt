package no.nav.syfo.kontaktinfo

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.brukertilgang.BrukertilgangService
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.fodselsnummerInvalid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/kontaktinfo"])
class KontaktinfoController(
    private val contextHolder: TokenValidationContextHolder,
    private val brukertilgangService: BrukertilgangService,
    private val krrClient: KrrClient,
    @Value("\${OPPFOLGINGSPLAN_FRONTEND_CLIENT_ID}")
    private val oppfolgingsplanClientId: String,
) {
    private val log = LoggerFactory.getLogger(KontaktinfoController::class.java)

    @ResponseBody
    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    fun getKontaktinfo(@RequestHeader(NAV_PERSONIDENT_HEADER) fnr: String): ResponseEntity<Kontaktinfo> {
        val innloggetFnr =
            TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId).fnrFromIdportenTokenX().value

        return when {
            fodselsnummerInvalid(fnr) -> {
                log.error("Ugyldig fnr ved henting av kontaktinfo")
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }

            !brukertilgangService.tilgangTilOppslattIdent(innloggetFnr, fnr) -> {
                log.error("Ikke tilgang til kontaktinfo: Bruker spør om noen andre enn seg selv eller egne ansatte")
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }

            else -> {
                val kontaktinfo = krrClient.kontaktinformasjon(fnr)
                ResponseEntity.ok(kontaktinfo.toKontaktinfo(fnr))
            }
        }
    }
}
