package no.nav.syfo.oppfolgingsplan.service

import jakarta.ws.rs.ForbiddenException
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class ArbeidsoppgaveService (
    pdlConsumer: PdlConsumer,
    arbeidsoppgaveDAO: ArbeidsoppgaveDAO,
    godkjenningerDAO: GodkjenningerDAO,
    metrikk: Metrikk,
    oppfolgingsplanDAO: OppfolgingsplanDAO,
    private val tilgangskontrollService: TilgangskontrollService
) {
    private val pdlConsumer: PdlConsumer = pdlConsumer
    private val arbeidsoppgaveDAO: ArbeidsoppgaveDAO = arbeidsoppgaveDAO
    private val godkjenningerDAO: GodkjenningerDAO = godkjenningerDAO
    private val metrikk: Metrikk = metrikk
    private val oppfolgingsplanDAO: OppfolgingsplanDAO = oppfolgingsplanDAO

    @Transactional
    @Throws(ConflictException::class)
    fun lagreArbeidsoppgave(oppfoelgingsdialogId: Long?, arbeidsoppgave: Arbeidsoppgave, fnr: String): Long {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfoelgingsdialogId)
        val innloggetAktoerId: String = pdlConsumer.aktorid(fnr)

        if (!eksisterendeArbeidsoppgaveHoererTilDialog(
                arbeidsoppgave.id,
                arbeidsoppgaveDAO.arbeidsoppgaverByOppfoelgingsdialogId(oppfoelgingsdialogId)
            )
            || !tilgangskontrollService.brukerTilhorerOppfolgingsplan(fnr, oppfolgingsplan)
        ) {
            throw ForbiddenException("Ikke tilgang")
        }

        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(oppfoelgingsdialogId).stream()
                .anyMatch { pGodkjenning -> pGodkjenning.godkjent }
        ) {
            throw ConflictException()
        }

        oppfolgingsplanDAO.sistEndretAv(oppfoelgingsdialogId, innloggetAktoerId)
        if (arbeidsoppgave.id == null) {
            metrikk.tellHendelse("lagre_arbeidsoppgave_ny")
            return arbeidsoppgaveDAO.create(
                arbeidsoppgave
                    .oppfoelgingsdialogId(oppfoelgingsdialogId)
                    .erVurdertAvSykmeldt(oppfolgingsplan.arbeidstaker.aktoerId.equals(innloggetAktoerId))
                    .opprettetAvAktoerId(innloggetAktoerId)
                    .sistEndretAvAktoerId(innloggetAktoerId)
            ).id
        } else {
            metrikk.tellHendelse("lagre_arbeidsoppgave_eksisterende")
            return arbeidsoppgaveDAO.update(
                arbeidsoppgave
                    .oppfoelgingsdialogId(oppfoelgingsdialogId)
                    .erVurdertAvSykmeldt(
                        oppfolgingsplan.arbeidstaker.aktoerId.equals(innloggetAktoerId) || arbeidsoppgaveDAO.finnArbeidsoppgave(
                            arbeidsoppgave.id
                        ).erVurdertAvSykmeldt
                    )
                    .sistEndretAvAktoerId(innloggetAktoerId)
            ).id
        }
    }

    @Transactional
    @Throws(ConflictException::class)
    fun slettArbeidsoppgave(arbeidsoppgaveId: Long?, fnr: String) {
        val innloggetAktoerId: String = pdlConsumer.aktorid(fnr)
        val arbeidsoppgave: Arbeidsoppgave = arbeidsoppgaveDAO.finnArbeidsoppgave(arbeidsoppgaveId)
        val oppfolgingsplan: Oppfolgingsplan =
            oppfolgingsplanDAO.finnOppfolgingsplanMedId(arbeidsoppgave.oppfoelgingsdialogId)

        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(fnr, oppfolgingsplan) || !kanEndreElement(
                innloggetAktoerId,
                oppfolgingsplan.arbeidstaker.aktoerId,
                arbeidsoppgave.opprettetAvAktoerId
            )
        ) {
            throw ForbiddenException("Ikke tilgang")
        }
        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(arbeidsoppgave.oppfoelgingsdialogId).stream()
                .anyMatch { pGodkjenning -> pGodkjenning.godkjent }
        ) {
            throw ConflictException()
        }

        oppfolgingsplanDAO.sistEndretAv(arbeidsoppgave.oppfoelgingsdialogId, innloggetAktoerId)
        arbeidsoppgaveDAO.delete(arbeidsoppgave.id)
    }
}
