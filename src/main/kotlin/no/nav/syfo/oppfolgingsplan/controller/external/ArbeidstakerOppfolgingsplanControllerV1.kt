package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.aareg.service.ArbeidsforholdService
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.oppfolgingsplan.domain.OppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplan.service.OppfolgingsplanService
import no.nav.syfo.pdl.PdlClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/arbeidstaker/oppfolgingsplaner"])
class ArbeidstakerOppfolgingsplanControllerV1 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val oppfolgingsplanService: OppfolgingsplanService,
    private val arbeidsforholdService: ArbeidsforholdService,
    private val pdlClient: PdlClient,
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
        val arbeidstakersOppfolgingsplaner: List<OppfolgingsplanDTO> =
            oppfolgingsplanService.arbeidstakersOppfolgingsplaner(innloggetIdent)
        val liste = arbeidstakersOppfolgingsplaner.map { it.toBrukerOppfolgingsplan(pdlClient) }
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
