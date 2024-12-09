package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.aareg.service.ArbeidsforholdService
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.narmesteleder.NarmesteLederClient
import no.nav.syfo.oppfolgingsplan.domain.BrukerOppfolgingsplan
import no.nav.syfo.oppfolgingsplan.domain.populerArbeidstakersStillinger
import no.nav.syfo.oppfolgingsplan.domain.populerPlanerMedAvbruttPlanListe
import no.nav.syfo.oppfolgingsplan.domain.toBrukerOppfolgingsplan
import no.nav.syfo.oppfolgingsplan.service.OppfolgingsplanService
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import java.nio.file.AccessDeniedException
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/arbeidsgiver/oppfolgingsplaner"])
class ArbeidsgiverOppfolgingsplanControllerV1 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val oppfolgingsplanService: OppfolgingsplanService,
    private val arbeidsforholdService: ArbeidsforholdService,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
    private val narmesteLederClient: NarmesteLederClient,
) {

    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    fun hentArbeidsgiversOppfolgingsplanerPaPersonident(
        @RequestHeader(name = NAV_PERSONIDENT_HEADER) personident: String,
        @RequestParam("virksomhetsnummer") virksomhetsnummer: String
    ): List<BrukerOppfolgingsplan> {
        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        val arbeidsgiversOppfolgingsplaner =
            oppfolgingsplanService.arbeidsgiversOppfolgingsplanerPaFnr(innloggetIdent, personident, virksomhetsnummer)
                .map { it.toBrukerOppfolgingsplan() }
        arbeidsgiversOppfolgingsplaner.forEach { plan -> plan.populerPlanerMedAvbruttPlanListe(arbeidsgiversOppfolgingsplaner) }
        val arbeidsforhold =
            arbeidsforholdService.arbeidstakersStillingerForOrgnummer(personident, listOf(virksomhetsnummer))
        arbeidsgiversOppfolgingsplaner.forEach { plan -> plan.populerArbeidstakersStillinger(arbeidsforhold) }
        return arbeidsgiversOppfolgingsplaner
    }

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun opprettOppfolgingsplanSomArbeidsgiver(@RequestBody opprettOppfolgingsplan: OpprettOppfolgingsplanRequest): Long {
        val innloggetFnr = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        val sykmeldtFnr = opprettOppfolgingsplan.sykmeldtFnr
        return if (narmesteLederClient.erNaermesteLederForAnsatt(innloggetFnr, sykmeldtFnr)) {
            val id = oppfolgingsplanService.opprettOppfolgingsplan(
                innloggetFnr,
                opprettOppfolgingsplan.virksomhetsnummer,
                sykmeldtFnr
            )
            id
        } else {
            throw AccessDeniedException("Innlogget bruker er ikke nærmeste leder for sykmeldt")
        }
    }
}
