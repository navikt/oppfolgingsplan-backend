package no.nav.syfo.oppfolgingsplan.service

import jakarta.ws.rs.ForbiddenException
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class KommentarService(
    pdlConsumer: PdlConsumer,
    godkjenningerDAO: GodkjenningerDAO,
    kommentarDAO: KommentarDAO,
    oppfolgingsplanDAO: OppfolgingsplanDAO,
    tiltakDAO: TiltakDAO,
    private val tilgangskontrollService: TilgangskontrollService
) {
    private val pdlConsumer: PdlConsumer = pdlConsumer
    private val godkjenningerDAO: GodkjenningerDAO = godkjenningerDAO
    private val kommentarDAO: KommentarDAO = kommentarDAO
    private val oppfolgingsplanDAO: OppfolgingsplanDAO = oppfolgingsplanDAO
    private val tiltakDAO: TiltakDAO = tiltakDAO

    @Transactional
    fun lagreKommentar(tiltakId: Long?, kommentar: Kommentar, fnr: String?): Long {
        val tiltak: Tiltak = tiltakDAO.finnTiltakById(tiltakId)
        val innloggetAktoerId: String = pdlConsumer.aktorid(fnr)

        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(tiltak.oppfoelgingsdialogId)
        if (kommentarenErIkkeOpprettetAvNoenAndre(kommentar, innloggetAktoerId, oppfolgingsplan)) {
            throw ForbiddenException("Ikke tilgang")
        }

        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(tiltak.oppfoelgingsdialogId).stream()
                .anyMatch { pGodkjenning -> pGodkjenning.godkjent }
        ) {
            throw ConflictException()
        }

        oppfolgingsplanDAO.sistEndretAv(tiltak.oppfoelgingsdialogId, innloggetAktoerId)
        return if (kommentar.id == null) {
            kommentarDAO.create(
                kommentar
                    .tiltakId(tiltakId)
                    .opprettetAvAktoerId(innloggetAktoerId)
                    .sistEndretAvAktoerId(innloggetAktoerId)
            ).id
        } else {
            kommentarDAO.update(kommentar).id
        }
    }

    private fun kommentarenErIkkeOpprettetAvNoenAndre(
        kommentar: Kommentar,
        innloggetAktoerId: String,
        oppfolgingsplan: Oppfolgingsplan
    ): Boolean {
        return kommentar.id != null && oppfolgingsplan.tiltakListe.stream().noneMatch { pTiltak ->
            pTiltak.opprettetAvAktoerId.equals(
                innloggetAktoerId
            )
        }
    }

    @Transactional
    fun slettKommentar(kommentarId: Long?, fnr: String) {
        val innloggetAktoerId: String = pdlConsumer.aktorid(fnr)
        val kommentar: Kommentar = kommentarDAO.finnKommentar(kommentarId)
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.oppfolgingsplanByTiltakId(kommentar.tiltakId)

        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(fnr, oppfolgingsplan) || !kanEndreElement(
                innloggetAktoerId,
                oppfolgingsplan.arbeidstaker.aktoerId,
                kommentar.opprettetAvAktoerId
            )
        ) {
            throw ForbiddenException("Ikke tilgang")
        }

        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(oppfolgingsplan.id).stream()
                .anyMatch { godkjenning -> godkjenning.godkjent }
        ) {
            throw ConflictException()
        }

        oppfolgingsplanDAO.sistEndretAv(oppfolgingsplan.id, innloggetAktoerId)
        kommentarDAO.delete(kommentarId)
    }
}
