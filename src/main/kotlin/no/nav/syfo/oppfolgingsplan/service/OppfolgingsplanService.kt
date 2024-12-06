package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.oppfolgingsplan.repository.dao.ArbeidsoppgaveDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.GodkjenningerDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.GodkjentplanDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.KommentarDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.OppfolgingsplanDAO
import no.nav.syfo.oppfolgingsplan.repository.dao.TiltakDAO
import no.nav.syfo.pdl.PdlClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject

@Service
class OppfolgingsplanService @Inject constructor(
    arbeidsoppgaveDAO: ArbeidsoppgaveDAO,
    dokumentDAO: DokumentDAO,
    kommentarDAO: KommentarDAO,
    godkjenningerDAO: GodkjenningerDAO,
    godkjentplanDAO: GodkjentplanDAO,
    oppfolgingsplanDAO: OppfolgingsplanDAO,
    tiltakDAO: TiltakDAO,
    dialogmeldingService: DialogmeldingService,
    pdlClient: PdlClient,
    private val tilgangskontrollService: TilgangskontrollService
) {
    private val oppfolgingsplanDAO: OppfolgingsplanDAO = oppfolgingsplanDAO

    private val arbeidsoppgaveDAO: ArbeidsoppgaveDAO = arbeidsoppgaveDAO

    private val godkjentplanDAO: GodkjentplanDAO = godkjentplanDAO

    private val tiltakDAO: TiltakDAO = tiltakDAO

    private val dialogmeldingService: DialogmeldingService = dialogmeldingService

    private val pdlConsumer: PdlConsumer = pdlConsumer

    private val godkjenningerDAO: GodkjenningerDAO = godkjenningerDAO

    private val kommentarDAO: KommentarDAO = kommentarDAO

    private val dokumentDAO: DokumentDAO = dokumentDAO

    fun arbeidsgiversOppfolgingsplanerPaFnr(
        lederFnr: String?,
        ansattFnr: String?,
        virksomhetsnummer: String
    ): List<Oppfolgingsplan> {
        val lederAktorId: String = pdlConsumer.aktorid(lederFnr)
        val ansattAktorId: String = pdlConsumer.aktorid(ansattFnr)

        if (!tilgangskontrollService.erNaermesteLederForSykmeldt(lederFnr, ansattFnr, virksomhetsnummer)) {
            throw ForbiddenException("Ikke tilgang")
        }

        return oppfolgingsplanDAO.oppfolgingsplanerKnyttetTilSykmeldtogVirksomhet(ansattAktorId, virksomhetsnummer)
            .stream()
            .map(oppfolgingsplanDAO::populate)
            .peek { oppfolgingsplan -> oppfolgingsplanDAO.oppdaterSistAksessert(oppfolgingsplan, lederAktorId) }
            .collect(Collectors.toList<T>())
    }

    fun arbeidstakersOppfolgingsplaner(innloggetFnr: String?): List<Oppfolgingsplan> {
        val innloggetAktorId: String = pdlConsumer.aktorid(innloggetFnr)
        return oppfolgingsplanDAO.oppfolgingsplanerKnyttetTilSykmeldt(innloggetAktorId).stream()
            .peek { oppfolgingsplan -> oppfolgingsplanDAO.oppdaterSistAksessert(oppfolgingsplan, innloggetAktorId) }
            .map(oppfolgingsplanDAO::populate)
            .collect(Collectors.toList<T>())
    }

    fun hentGodkjentOppfolgingsplan(oppfoelgingsdialogId: Long?): Oppfolgingsplan {
        return oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfoelgingsdialogId)
            .godkjentPlan(godkjentplanDAO.godkjentPlanByOppfolgingsplanId(oppfoelgingsdialogId))
    }

    @Transactional
    fun opprettOppfolgingsplan(innloggetFnr: String, virksomhetsnummer: String, sykmeldtFnr: String): Long {
        val innloggetAktoerId: String = pdlConsumer.aktorid(innloggetFnr)
        val sykmeldtAktoerId = if (innloggetFnr == sykmeldtFnr)
            innloggetAktoerId
        else
            pdlConsumer.aktorid(sykmeldtFnr)

        if (!tilgangskontrollService.kanOppretteOppfolgingsplan(sykmeldtFnr, innloggetFnr, virksomhetsnummer)) {
            throw ForbiddenException("Ikke tilgang")
        }

        if (parteneHarEkisterendeAktivOppfolgingsplan(sykmeldtAktoerId, virksomhetsnummer)) {
            log.warn("Kan ikke opprette en plan når det allerede eksisterer en aktiv plan mellom partene!")
            throw ConflictException()
        }

        return opprettDialog(sykmeldtAktoerId, sykmeldtFnr, virksomhetsnummer, innloggetAktoerId, innloggetFnr)
    }

    @Transactional
    fun kopierOppfoelgingsdialog(oppfoelgingsdialogId: Long, innloggetFnr: String): Long {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfoelgingsdialogId)
        val innloggetAktoerId: String = pdlConsumer.aktorid(innloggetFnr)

        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(innloggetFnr, oppfolgingsplan)) {
            throw ForbiddenException()
        }
        val sykmeldtAktoerId: String = oppfolgingsplan.arbeidstaker.aktoerId
        val sykmeldtFnr: String =
            if (oppfolgingsplan.arbeidstaker.fnr != null) oppfolgingsplan.arbeidstaker.fnr else pdlConsumer.fnr(
                sykmeldtAktoerId
            )

        if (parteneHarEkisterendeAktivOppfolgingsplan(sykmeldtAktoerId, oppfolgingsplan.virksomhet.virksomhetsnummer)) {
            log.warn("Kan ikke opprette en plan når det allerede eksisterer en aktiv plan mellom partene!")
            throw ConflictException()
        }

        val nyOppfoelgingsdialogId = opprettDialog(
            oppfolgingsplan.arbeidstaker.aktoerId,
            sykmeldtFnr,
            oppfolgingsplan.virksomhet.virksomhetsnummer,
            innloggetAktoerId,
            innloggetFnr
        )
        overfoerDataFraDialogTilNyDialog(oppfoelgingsdialogId, nyOppfoelgingsdialogId)

        return nyOppfoelgingsdialogId
    }

    @Transactional
    fun overfoerDataFraDialogTilNyDialog(gammelOppfoelgingsdialogId: Long, nyOppfoelgingsdialogId: Long) {
        arbeidsoppgaveDAO.arbeidsoppgaverByOppfoelgingsdialogId(gammelOppfoelgingsdialogId)
            .forEach { arbeidsoppgave ->
                arbeidsoppgaveDAO.create(
                    arbeidsoppgave.oppfoelgingsdialogId(
                        nyOppfoelgingsdialogId
                    )
                )
            }
        tiltakDAO.finnTiltakByOppfoelgingsdialogId(gammelOppfoelgingsdialogId)
            .forEach { tiltak ->
                val nyttTiltak: Tiltak = tiltakDAO.create(tiltak.oppfoelgingsdialogId(nyOppfoelgingsdialogId))
                tiltak.kommentarer.forEach { kommentar -> kommentarDAO.create(kommentar.tiltakId(nyttTiltak.id)) }
            }
    }

    private fun opprettDialog(
        sykmeldtAktorId: String,
        sykmeldtFnr: String,
        virksomhetsnummer: String,
        innloggetAktorId: String,
        innloggetFnr: String
    ): Long {
        var oppfolgingsplan: Oppfolgingsplan = Oppfolgingsplan()
            .sistEndretAvAktoerId(innloggetAktorId)
            .sistEndretAvFnr(innloggetFnr)
            .opprettetAvAktoerId(innloggetAktorId)
            .opprettetAvFnr(innloggetFnr)
            .arbeidstaker(Person().aktoerId(sykmeldtAktorId).fnr(sykmeldtFnr))
            .virksomhet(Virksomhet().virksomhetsnummer(virksomhetsnummer))
        if (innloggetAktorId == sykmeldtAktorId) {
            oppfolgingsplan.arbeidstaker.sistInnlogget(LocalDateTime.now())
            oppfolgingsplan.arbeidstaker.sisteEndring(LocalDateTime.now())
            oppfolgingsplan.arbeidstaker.sistAksessert(LocalDateTime.now())
        } else {
            oppfolgingsplan.arbeidsgiver.sistInnlogget(LocalDateTime.now())
            oppfolgingsplan.arbeidsgiver.sisteEndring(LocalDateTime.now())
            oppfolgingsplan.arbeidsgiver.sistAksessert(LocalDateTime.now())
        }
        oppfolgingsplan = oppfolgingsplanDAO.create(oppfolgingsplan)
        return oppfolgingsplan.id
    }

    private fun parteneHarEkisterendeAktivOppfolgingsplan(
        sykmeldtAktoerId: String,
        virksomhetsnummer: String
    ): Boolean {
        return oppfolgingsplanDAO.oppfolgingsplanerKnyttetTilSykmeldtogVirksomhet(sykmeldtAktoerId, virksomhetsnummer)
            .stream()
            .map { oppfoelgingsdialog -> godkjentplanDAO.godkjentPlanByOppfolgingsplanId(oppfoelgingsdialog.id) }
            .anyMatch { maybeGodkjentplan ->
                erIkkeFerdigVersjon(maybeGodkjentplan) || erIkkeAvbruttOgIkkeUtgaatt(
                    maybeGodkjentplan
                )
            }
    }

    private fun erIkkeAvbruttOgIkkeUtgaatt(maybeGodkjentplan: Optional<GodkjentPlan>): Boolean {
        return maybeGodkjentplan.isPresent()
                && maybeGodkjentplan.get().avbruttPlan.isEmpty()
                && maybeGodkjentplan.get().gyldighetstidspunkt.tom.isAfter(LocalDate.now())
    }

    private fun erIkkeFerdigVersjon(maybeGodkjentplan: Optional<GodkjentPlan>): Boolean {
        return maybeGodkjentplan.isEmpty()
    }

    @Transactional
    fun delMedNav(oppfolgingsplanId: Long, innloggetFnr: String) {
        val oppfoelgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)
        val innloggetAktoerId: String = pdlConsumer.aktorid(innloggetFnr)
        val deltMedNavTidspunkt = LocalDateTime.now()

        throwExceptionWithoutAccessToOppfolgingsplan(innloggetFnr, oppfoelgingsplan)

        godkjentplanDAO.delMedNav(oppfolgingsplanId, deltMedNavTidspunkt)
        godkjentplanDAO.delMedNavTildelEnhet(oppfoelgingsplan.id)
        oppfolgingsplanDAO.sistEndretAv(oppfolgingsplanId, innloggetAktoerId)
    }

    @Transactional
    fun delMedFastlege(oppfolgingsplanId: Long, innloggetFnr: String) {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)

        throwExceptionWithoutAccessToOppfolgingsplan(innloggetFnr, oppfolgingsplan)

        val arbeidstakerAktoerId: String = oppfolgingsplan.arbeidstaker.aktoerId
        val arbeidstakerFnr: String = pdlConsumer.fnr(arbeidstakerAktoerId)

        val pdf: ByteArray = godkjentplanDAO.godkjentPlanByOppfolgingsplanId(oppfolgingsplanId)
            .map(GodkjentPlan::dokumentUuid)
            .map(dokumentDAO::hent)
            .orElseThrow { RuntimeException("Finner ikke pdf for oppfølgingsplan med id $oppfolgingsplanId") }

        dialogmeldingService.sendOppfolgingsplanTilFastlege(arbeidstakerFnr, pdf)

        godkjentplanDAO.delMedFastlege(oppfolgingsplanId)
    }

    @Transactional
    fun nullstillGodkjenning(oppfolgingsplanId: Long, fnr: String) {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)
        val innloggetAktoerId: String = pdlConsumer.aktorid(fnr)

        throwExceptionWithoutAccessToOppfolgingsplan(fnr, oppfolgingsplan)

        oppfolgingsplanDAO.sistEndretAv(oppfolgingsplanId, innloggetAktoerId)
        oppfolgingsplanDAO.nullstillSamtykke(oppfolgingsplanId)
        godkjenningerDAO.deleteAllByOppfoelgingsdialogId(oppfolgingsplanId)
    }

    fun oppdaterSistInnlogget(oppfolgingsplanId: Long, fnr: String) {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)
        val innloggetAktoerId: String = pdlConsumer.aktorid(fnr)

        throwExceptionWithoutAccessToOppfolgingsplan(fnr, oppfolgingsplan)

        oppfolgingsplanDAO.oppdaterSistInnlogget(oppfolgingsplan, innloggetAktoerId)
    }

    @Transactional
    fun avbrytPlan(oppfolgingsplanId: Long, innloggetFnr: String): Long {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)
        val innloggetAktoerId: String = pdlConsumer.aktorid(innloggetFnr)

        throwExceptionWithoutAccessToOppfolgingsplan(innloggetFnr, oppfolgingsplan)

        oppfolgingsplanDAO.avbryt(oppfolgingsplan.id, innloggetAktoerId)
        val nyOppfolgingsplanId = opprettDialog(
            oppfolgingsplan.arbeidstaker.aktoerId,
            oppfolgingsplan.arbeidstaker.fnr,
            oppfolgingsplan.virksomhet.virksomhetsnummer,
            innloggetAktoerId,
            innloggetFnr
        )
        overfoerDataFraDialogTilNyDialog(oppfolgingsplanId, nyOppfolgingsplanId)
        return nyOppfolgingsplanId
    }

    fun harBrukerTilgangTilDialog(oppfolgingsplanId: Long, fnr: String): Boolean {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)
        return tilgangskontrollService.brukerTilhorerOppfolgingsplan(fnr, oppfolgingsplan)
    }

    private fun throwExceptionWithoutAccessToOppfolgingsplan(fnr: String, oppfolgingsplan: Oppfolgingsplan) {
        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(fnr, oppfolgingsplan)) {
            throw ForbiddenException()
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OppfolgingsplanService::class.java)
    }
}
