package no.nav.syfo.aareg.service

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.syfo.aareg.model.Stilling
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.logger
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
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
    private val arbeidsforholdService: ArbeidsforholdService,
) {
    private val log = logger()

    @ResponseBody
    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    fun getArbeidstakersStillingerForOrgnummer(
        @RequestHeader(NAV_PERSONIDENT_HEADER) aktorId: String,
        @RequestParam date: LocalDate,
        @RequestParam orgnummer: String
    ): ResponseEntity<List<Stilling>> {
        val stillinger = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(aktorId, date, orgnummer)
        stillinger.forEach { log.info("Stilling: {${it.orgnummer}, ${it.fom}, ${it.tom}, ${it.yrke}, ${it.prosent}") }
        return ResponseEntity.ok(stillinger)
    }
}
