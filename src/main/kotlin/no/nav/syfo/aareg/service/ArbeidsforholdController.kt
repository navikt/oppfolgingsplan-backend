package no.nav.syfo.aareg.service

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.aareg.model.Stilling
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.brukertilgang.BrukertilgangService
import no.nav.syfo.logger
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.fodselsnummerInvalid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/arbeidsforhold"])
class ArbeidsforholdController(
    private val contextHolder: TokenValidationContextHolder,
    private val arbeidsforholdService: ArbeidsforholdService,
    private val brukertilgangService: BrukertilgangService,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
) {
    private val log = logger()

    @ResponseBody
    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    fun getArbeidstakersStillingerForOrgnummer(
        @RequestHeader(NAV_PERSONIDENT_HEADER) fnr: String,
        @RequestParam date: LocalDate,
        @RequestParam orgnummer: String
    ): ResponseEntity<List<Stilling>> {
        val innloggetFnr = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        return if (fodselsnummerInvalid(fnr)) {
            log.error("Ugyldig fnr ved henting av person")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .build()
        } else {
            if (!brukertilgangService.tilgangTilOppslattIdent(innloggetFnr, fnr)) {
                log.error("Ikke tilgang til person: Bruker spør om noen andre enn seg selv eller egne ansatte")
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build()
            } else {
                val stillinger = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(fnr, date, orgnummer)
                log.info("Hentet ${stillinger.size} stillinger")
                ResponseEntity.ok(stillinger)
            }
        }
    }
}
