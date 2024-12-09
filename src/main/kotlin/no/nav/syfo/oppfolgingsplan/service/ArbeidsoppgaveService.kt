package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.metric.Metrikk
import no.nav.syfo.oppfolgingsplan.domain.ArbeidsoppgaveDTO
import no.nav.syfo.oppfolgingsplan.repository.dao.ArbeidsoppgaveDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.GodkjenningerDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.OppfolgingsplanDAO
import no.nav.syfo.oppfolgingsplan.util.eksisterendeArbeidsoppgaveHoererTilDialog
import no.nav.syfo.oppfolgingsplan.util.kanEndreElement
import no.nav.syfo.pdl.PdlClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ArbeidsoppgaveService(
    private val pdlClient: PdlClient,
    private val arbeidsoppgaveDAO: ArbeidsoppgaveDAO,
    private val godkjenningerDAO: GodkjenningerDAO,
    private val metrikk: Metrikk,
    private val oppfolgingsplanDAO: OppfolgingsplanDAO,
    private val tilgangskontrollService: TilgangskontrollService
) {

    @Transactional
    @Throws(ResponseStatusException::class)
    fun lagreArbeidsoppgave(oppfoelgingsdialogId: Long, arbeidsoppgave: ArbeidsoppgaveDTO, innloggetFnr: String): Long {
        val oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfoelgingsdialogId)
            ?: throw IllegalArgumentException("Fant ikke oppfølgingsplan")
        val innloggetAktoerId = pdlClient.aktorid(innloggetFnr)

        if (!eksisterendeArbeidsoppgaveHoererTilDialog(
                arbeidsoppgave.id,
                arbeidsoppgaveDAO.arbeidsoppgaverByOppfoelgingsdialogId(oppfoelgingsdialogId)
            ) || !tilgangskontrollService.brukerTilhorerOppfolgingsplan(innloggetFnr, oppfolgingsplan)
        ) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke tilgang: Arbeidsoppave hører ikke til dialog")
        }

        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(oppfoelgingsdialogId).any { it.godkjent }) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Conflict: Already godkjent")
        }

        oppfolgingsplanDAO.sistEndretAv(oppfoelgingsdialogId, innloggetAktoerId)
        return if (arbeidsoppgave.id == null) {
            metrikk.tellHendelse("lagre_arbeidsoppgave_ny")
            arbeidsoppgaveDAO.create(
                arbeidsoppgave.copy(
                    oppfoelgingsdialogId = oppfoelgingsdialogId,
                    erVurdertAvSykmeldt = oppfolgingsplan.arbeidstaker.fnr == innloggetFnr,
                    opprettetAvAktoerId = innloggetAktoerId,
                    sistEndretAvAktoerId = innloggetAktoerId
                )
            ).id ?: throw IllegalStateException("ID should not be null")
        } else {
            metrikk.tellHendelse("lagre_arbeidsoppgave_eksisterende")
            arbeidsoppgaveDAO.update(
                arbeidsoppgave.copy(
                    oppfoelgingsdialogId = oppfoelgingsdialogId,
                    erVurdertAvSykmeldt = oppfolgingsplan.arbeidstaker.fnr == innloggetFnr || arbeidsoppgaveDAO.finnArbeidsoppgave(
                        arbeidsoppgave.id
                    )!!.erVurdertAvSykmeldt,
                    sistEndretAvAktoerId = innloggetAktoerId
                )
            ).id ?: throw IllegalStateException("ID should not be null")
        }
    }

    @Transactional
    @Throws(ResponseStatusException::class)
    fun slettArbeidsoppgave(arbeidsoppgaveId: Long, innloggetFnr: String) {
        val innloggetAktoerId = pdlClient.aktorid(innloggetFnr)
        val arbeidsoppgave = arbeidsoppgaveDAO.finnArbeidsoppgave(arbeidsoppgaveId)
            ?: throw IllegalArgumentException("Fant ikke arbeidsoppgave")
        val oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(arbeidsoppgave.oppfoelgingsdialogId)
            ?: throw IllegalArgumentException("Fant ikke oppfølgingsplan")

        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(innloggetFnr, oppfolgingsplan) || !kanEndreElement(
                innloggetFnr,
                oppfolgingsplan.arbeidstaker.aktoerId,
                arbeidsoppgave.opprettetAvAktoerId
            )
        ) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke tilgang")
        }
        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(arbeidsoppgave.oppfoelgingsdialogId)
                .any { it.godkjent }
        ) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Conflict: Already godkjent")
        }

        oppfolgingsplanDAO.sistEndretAv(arbeidsoppgave.oppfoelgingsdialogId, innloggetAktoerId)
        arbeidsoppgaveDAO.delete(arbeidsoppgave.id)
    }
}