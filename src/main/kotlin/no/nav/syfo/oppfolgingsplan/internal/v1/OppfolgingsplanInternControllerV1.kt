package no.nav.syfo.oppfolgingsplan.internal.v1

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.syfo.auth.oidc.OIDCIssuer.INTERN_AZUREAD_V2
import no.nav.syfo.domain.Fodselsnummer
import no.nav.syfo.ereg.EregClient
import no.nav.syfo.exception.NameNotFoundException
import no.nav.syfo.oppfolgingsplan.domain.Historikk
import no.nav.syfo.oppfolgingsplan.domain.OppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplan.domain.rs.RSOppfoelgingsdialog
import no.nav.syfo.oppfolgingsplan.mapper.oppfoelgingsdialog2rs
import no.nav.syfo.oppfolgingsplan.repository.dao.OppfolgingsplanDAO
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.pdl.fullName
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = INTERN_AZUREAD_V2)
@RequestMapping(value = ["/api/internad/v1/oppfolgingsplan"])
class OppfolgingsplanInternControllerV1(
    private val pdlClient: PdlClient,
    private val eregClient: EregClient,
    private val oppfolgingsplanDAO: OppfolgingsplanDAO,
    private val veilederTilgangClient: VeilederTilgangClient
) {
    private fun finnDeltAvNavn(oppfolgingsplan: OppfolgingsplanDTO): String {
        val arbeidstakersAktoerId = oppfolgingsplan.arbeidstaker.aktoerId
        if (oppfolgingsplan.sistEndretAvAktoerId == arbeidstakersAktoerId) {
            return pdlClient.person(arbeidstakersAktoerId!!)?.fullName()
                ?: throw NameNotFoundException("Name from PDL not found")
        }
        return eregClient.virksomhetsnavn(oppfolgingsplan.virksomhet.virksomhetsnummer)
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @RequestMapping(value = ["/historikk"])
    fun getHistorikk(@RequestHeader(name = NAV_PERSONIDENT_HEADER) personident: String): List<Historikk> {
        val personFnr = Fodselsnummer(personident)

        veilederTilgangClient.throwExceptionIfVeilederWithoutAccessWithOBO(personFnr)

        val godkjentePlanerSomErDeltMedNAV = oppfolgingsplanDAO.oppfolgingsplanerKnyttetTilSykmeldt(personFnr.value)
            .mapNotNull { oppfolgingsplanDAO.populate(it).takeIf { plan -> plan.godkjentPlan?.deltMedNAV == true } }

        return godkjentePlanerSomErDeltMedNAV.map {
            Historikk(
                tekst = "Oppfølgingsplanen ble delt med NAV av ${finnDeltAvNavn(it)}.",
                tidspunkt = it.godkjentPlan?.deltMedNAVTidspunkt
            )
        }
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOppfolgingsplaner(
        @RequestHeader(name = NAV_PERSONIDENT_HEADER) personident: String
    ): List<RSOppfoelgingsdialog> {
        val personFnr = Fodselsnummer(personident)

        veilederTilgangClient.throwExceptionIfVeilederWithoutAccessWithOBO(personFnr)

        val godkjentePlanerSomErDeltMedNAV = oppfolgingsplanDAO.oppfolgingsplanerKnyttetTilSykmeldt(personFnr.value)
            .mapNotNull { oppfolgingsplanDAO.populate(it).takeIf { plan -> plan.godkjentPlan?.deltMedNAV == true } }

        return godkjentePlanerSomErDeltMedNAV.map { oppfoelgingsdialog2rs(it) }
    }
}
