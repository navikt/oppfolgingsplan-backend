package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.v2.domain.oppfolgingsplan.BrukerOppfolgingsplan
import no.nav.syfo.api.v2.domain.oppfolgingsplan.OpprettOppfolgingsplanRequest
import no.nav.syfo.api.v2.mapper.populerArbeidstakersStillinger
import no.nav.syfo.api.v2.mapper.populerPlanerMedAvbruttPlanListe
import no.nav.syfo.api.v2.mapper.toBrukerOppfolgingsplan
import no.nav.syfo.api.v2.mapper.toVirksomhetsnummer
import no.nav.syfo.domain.Oppfolgingsplan
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.service.ArbeidsforholdService
import no.nav.syfo.service.OppfolgingsplanService
import no.nav.syfo.tokenx.TokenXUtil
import no.nav.syfo.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.tokenx.TokenXUtil.fnrFromIdportenTokenX
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v2/arbeidstaker/oppfolgingsplaner"])
class ArbeidstakerOppfolgingsplanControllerV1 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val oppfolgingsplanService: OppfolgingsplanService,
    private val arbeidsforholdService: ArbeidsforholdService,
    private val pdlConsumer: PdlConsumer,
    private val metrikk: Metrikk,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
    @Value("\${ditt.sykefravaer.frontend.client.id}")
    private val dittSykefravaerClientId: String,
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentArbeidstakersOppfolgingsplaner(): List<BrukerOppfolgingsplan> {
        val innloggetIdent =
            TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId, dittSykefravaerClientId)
                .fnrFromIdportenTokenX()
                .value
        val arbeidstakersOppfolgingsplaner: List<Oppfolgingsplan> =
            oppfolgingsplanService.arbeidstakersOppfolgingsplaner(innloggetIdent)
        val liste = arbeidstakersOppfolgingsplaner.map { it.toBrukerOppfolgingsplan(pdlConsumer) }
        liste.forEach { plan -> plan.populerPlanerMedAvbruttPlanListe(liste) }
        val arbeidstakersStillinger = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(innloggetIdent, liste.toVirksomhetsnummer())
        liste.forEach { plan -> plan.populerArbeidstakersStillinger(arbeidstakersStillinger) }
        metrikk.tellHendelse("hent_oppfolgingsplan_at")
        return liste
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettOppfolgingsplanSomArbeidstaker(@RequestBody opprettOppfolgingsplan: OpprettOppfolgingsplanRequest): Long {
        val innloggetFnr = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        val id = oppfolgingsplanService.opprettOppfolgingsplan(innloggetFnr, opprettOppfolgingsplan.virksomhetsnummer, innloggetFnr)
        metrikk.tellHendelse("opprett_oppfolgingsplan_at")
        return id
    }
}
