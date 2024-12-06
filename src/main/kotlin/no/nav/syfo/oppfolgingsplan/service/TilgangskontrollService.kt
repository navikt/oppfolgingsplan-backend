package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.domain.Oppfolgingsplan
import no.nav.syfo.narmesteleder.NarmesteLederConsumer
import no.nav.syfo.pdl.PdlConsumer
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class TilgangskontrollService(
    narmesteLederConsumer: NarmesteLederConsumer,
    pdlConsumer: PdlConsumer
) {
    private val narmesteLederConsumer: NarmesteLederConsumer = narmesteLederConsumer
    private val pdlConsumer: PdlConsumer = pdlConsumer

    fun brukerTilhorerOppfolgingsplan(fnr: String, oppfolgingsplan: Oppfolgingsplan): Boolean {
        val arbeidstakersFnr: String = pdlConsumer.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        return arbeidstakersFnr == fnr
                || erNaermesteLederForSykmeldt(fnr, arbeidstakersFnr, oppfolgingsplan.virksomhet.virksomhetsnummer)
    }

    fun kanOppretteOppfolgingsplan(sykmeldtFnr: String, innloggetFnr: String, virksomhetsnummer: String): Boolean {
        return (innloggetFnr == sykmeldtFnr && aktoerHarNaermesteLederHosVirksomhet(innloggetFnr, virksomhetsnummer))
                || erNaermesteLederForSykmeldt(innloggetFnr, sykmeldtFnr, virksomhetsnummer)
    }

    fun erNaermesteLederForSykmeldt(lederFnr: String?, sykmeldtFnr: String?, virksomhetsnummer: String): Boolean {
        return narmesteLederConsumer.ansatte(lederFnr).stream()
            .anyMatch { ansatt -> virksomhetsnummer == ansatt.virksomhetsnummer && ansatt.fnr.equals(sykmeldtFnr) }
    }

    private fun aktoerHarNaermesteLederHosVirksomhet(fnr: String, virksomhetsnummer: String): Boolean {
        return narmesteLederConsumer.narmesteLeder(fnr, virksomhetsnummer).isPresent()
    }
}
