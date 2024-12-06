package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.dokarkiv.DokArkivConsumer
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class JournalforOPService @Inject constructor(
    oppfolgingsplanDAO: OppfolgingsplanDAO,
    dokArkivConsumer: DokArkivConsumer,
    eregConsumer: EregConsumer,
    private val brukerprofilService: BrukerprofilService,
    private val dokumentService: DokumentService
) {
    private val oppfolgingsplanDAO: OppfolgingsplanDAO = oppfolgingsplanDAO
    private val dokArkivConsumer: DokArkivConsumer = dokArkivConsumer
    private val eregConsumer: EregConsumer = eregConsumer

    fun opprettJournalpost(godkjentPlan: GodkjentPlan): Int {
        val oppfolgingsplan: Oppfolgingsplan =
            oppfolgingsplanDAO.finnOppfolgingsplanMedId(godkjentPlan.oppfoelgingsdialogId)
        val virksomhetsnavn: String = eregConsumer.virksomhetsnavn(oppfolgingsplan.virksomhet.virksomhetsnummer)
        oppfolgingsplan.virksomhet.navn(virksomhetsnavn)
        setArbeidstakerinfo(oppfolgingsplan)
        setPDF(godkjentPlan)

        return dokArkivConsumer.journalforOppfolgingsplan(oppfolgingsplan, godkjentPlan)
    }

    private fun setPDF(godkjentPlan: GodkjentPlan) {
        godkjentPlan.dokument = dokumentService.hentDokument(godkjentPlan.dokumentUuid)
    }

    private fun setArbeidstakerinfo(oppfoelgingsplan: Oppfolgingsplan) {
        val person: Person = brukerprofilService.hentNavnOgFnr(oppfoelgingsplan.arbeidstaker.aktoerId)
        oppfoelgingsplan.arbeidstaker.navn(person.navn)
        oppfoelgingsplan.arbeidstaker.fnr(person.fnr)
    }
}


