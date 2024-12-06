package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.metric.Metrikk
import no.nav.syfo.oppfolgingsplan.domain.OppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplan.domain.TiltakDTO
import no.nav.syfo.oppfolgingsplan.repository.dao.GodkjenningerDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.KommentarDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.OppfolgingsplanDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.TiltakDAO
import no.nav.syfo.oppfolgingsplan.util.eksisterendeTiltakHoererTilDialog
import no.nav.syfo.pdl.PdlClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.function.Consumer

@Service
class TiltakService(
    pdlClient: PdlClient,
    godkjenningerDAO: GodkjenningerDAO,
    kommentarDAO: KommentarDAO,
    metrikk: Metrikk,
    oppfolgingsplanDAO: OppfolgingsplanDAO,
    private val tilgangskontrollService: TilgangskontrollService,
    tiltakDAO: TiltakDAO
) {
    private val pdlClient: PdlClient = pdlClient
    private val godkjenningerDAO: GodkjenningerDAO = godkjenningerDAO
    private val kommentarDAO: KommentarDAO = kommentarDAO
    private val metrikk: Metrikk = metrikk
    private val oppfolgingsplanDAO: OppfolgingsplanDAO = oppfolgingsplanDAO
    private val tiltakDAO: TiltakDAO = tiltakDAO

    @Transactional
    fun lagreTiltak(oppfoelgingsdialogId: Long, tiltak: TiltakDTO, fnr: String): Long {
        val oppfolgingsplan: OppfolgingsplanDTO? = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfoelgingsdialogId)
        val innloggetAktoerId: String = pdlClient.aktorid(fnr)

        if (!eksisterendeTiltakHoererTilDialog(
                tiltak.id,
                tiltakDAO.finnTiltakByOppfoelgingsdialogId(oppfoelgingsdialogId)
            ) || !tilgangskontrollService.brukerTilhorerOppfolgingsplan(fnr, oppfolgingsplan)
        ) {
            throw AccessDeniedException("Ikke tilgang")
        }

        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(oppfoelgingsdialogId).stream()
                .anyMatch { pGodkjenning -> pGodkjenning.godkjent }
        ) {
            throw IllegalStateException()
        }

        oppfolgingsplanDAO.sistEndretAv(oppfoelgingsdialogId, innloggetAktoerId)
        if (tiltak.id == null) {
            metrikk.tellHendelse("lagre_tiltak_nytt")
            return tiltakDAO.create(
                tiltak.copy(
                    oppfoelgingsdialogId = oppfoelgingsdialogId,
                    opprettetAvAktoerId = innloggetAktoerId,
                    sistEndretAvAktoerId = innloggetAktoerId
                )
            ).id
        } else {
            metrikk.tellHendelse("lagre_tiltak_eksisterende")
            return tiltakDAO.create(
                tiltak.copy(
                    oppfoelgingsdialogId = oppfoelgingsdialogId,
                    sistEndretAvAktoerId = innloggetAktoerId
                )
            ).id
        }
    }

    @Transactional
    fun slettTiltak(tiltakId: Long?, fnr: String?) {
        val innloggetAktoerId: String = pdlConsumer.aktorid(fnr)
        val tiltak: Tiltak = tiltakDAO.finnTiltakById(tiltakId)
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(tiltak.oppfoelgingsdialogId)

        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(fnr, oppfolgingsplan) || !kanEndreElement(
                innloggetAktoerId,
                oppfolgingsplan.arbeidstaker.aktoerId,
                tiltak.opprettetAvAktoerId
            )
        ) {
            throw ForbiddenException("Ikke tilgang")
        }

        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(tiltak.oppfoelgingsdialogId).stream()
                .anyMatch { pGodkjenning -> pGodkjenning.godkjent }
        ) {
            throw ConflictException()
        }

        oppfolgingsplanDAO.sistEndretAv(tiltak.oppfoelgingsdialogId, innloggetAktoerId)
        val kommetarer: List<Kommentar> = kommentarDAO.finnKommentarerByTiltakId(tiltakId)
        kommetarer.forEach(Consumer<Kommentar> { kommentar: Kommentar -> kommentarDAO.delete(kommentar.id) })
        tiltakDAO.deleteById(tiltakId)
    }
}
