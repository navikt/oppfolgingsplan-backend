package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.narmesteleder.NarmesteLederClient
import no.nav.syfo.oppfolgingsplan.domain.OppfolgingsplanDTO
import no.nav.syfo.pdl.PdlClient
import org.springframework.stereotype.Service

@Service
class TilgangskontrollService(
    private val narmesteLederClient: NarmesteLederClient,
    private val pdlClient: PdlClient
) {

    fun brukerTilhorerOppfolgingsplan(fnr: String, oppfolgingsplan: OppfolgingsplanDTO): Boolean {
        requireNotNull(oppfolgingsplan.arbeidstaker.aktoerId) { "AktoerId for arbeidstaker mangler" }
        val arbeidstakersFnr: String = pdlClient.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        return arbeidstakersFnr == fnr ||
            erNaermesteLederForSykmeldt(fnr, arbeidstakersFnr, oppfolgingsplan.virksomhet.virksomhetsnummer)
    }

    fun kanOppretteOppfolgingsplan(sykmeldtFnr: String, innloggetFnr: String, virksomhetsnummer: String): Boolean {
        return (innloggetFnr == sykmeldtFnr && aktoerHarNaermesteLederHosVirksomhet(innloggetFnr, virksomhetsnummer)) ||
            erNaermesteLederForSykmeldt(innloggetFnr, sykmeldtFnr, virksomhetsnummer)
    }

    fun erNaermesteLederForSykmeldt(lederFnr: String?, sykmeldtFnr: String?, virksomhetsnummer: String): Boolean {
        if (lederFnr == null) return false
        return narmesteLederClient.ansatte(lederFnr)
            ?.any { ansatt -> virksomhetsnummer == ansatt.virksomhetsnummer && ansatt.fnr == sykmeldtFnr } ?: false
    }

    private fun aktoerHarNaermesteLederHosVirksomhet(fnr: String, virksomhetsnummer: String): Boolean {
        return narmesteLederClient.aktivNarmesteLederIVirksomhet(fnr, virksomhetsnummer) != null
    }
}
