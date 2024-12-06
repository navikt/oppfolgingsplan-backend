package no.nav.syfo.oppfolgingsplan.service

import jakarta.ws.rs.ForbiddenException
import no.nav.syfo.oppfolgingsplan.domain.GyldighetstidspunktDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject

@Service
class GodkjenningService(
    arbeidsforholdService: ArbeidsforholdService,
    metrikk: Metrikk,
    asynkOppgaveDAO: AsynkOppgaveDAO,
    dokumentDAO: DokumentDAO,
    godkjenningerDAO: GodkjenningerDAO,
    godkjentplanDAO: GodkjentplanDAO,
    oppfolgingsplanDAO: OppfolgingsplanDAO,
    pdlConsumer: PdlConsumer,
    private val brukerprofilService: BrukerprofilService,
    dkifConsumer: DkifConsumer,
    eregConsumer: EregConsumer,
    narmesteLederConsumer: NarmesteLederConsumer,
    esyfovarselService: EsyfovarselService,
    private val tilgangskontrollService: TilgangskontrollService
) {
    private val arbeidsforholdService: ArbeidsforholdService = arbeidsforholdService

    private val metrikk: Metrikk = metrikk

    private val oppfolgingsplanDAO: OppfolgingsplanDAO = oppfolgingsplanDAO

    private val narmesteLederConsumer: NarmesteLederConsumer = narmesteLederConsumer

    private val pdlConsumer: PdlConsumer = pdlConsumer

    private val dkifConsumer: DkifConsumer = dkifConsumer

    private val godkjentplanDAO: GodkjentplanDAO = godkjentplanDAO

    private val dokumentDAO: DokumentDAO = dokumentDAO

    private val eregConsumer: EregConsumer = eregConsumer

    private val esyfovarselService: EsyfovarselService = esyfovarselService

    private val godkjenningerDAO: GodkjenningerDAO = godkjenningerDAO

    private val asynkOppgaveDAO: AsynkOppgaveDAO = asynkOppgaveDAO

    @Transactional
    fun godkjennLederSinEgenOppfolgingsplan(
        oppfolgingsplanId: Long,
        gyldighetstidspunkt: GyldighetstidspunktDTO,
        innloggetFnr: String,
        delMedNav: Boolean
    ) {
        var oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)

        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(innloggetFnr, oppfolgingsplan)) {
            throw ForbiddenException("Ikke tilgang")
        }
        val innloggetAktoerId: String = pdlConsumer.aktorid(innloggetFnr)

        if (annenPartHarGjortEndringerImellomtiden(oppfolgingsplan, innloggetAktoerId)) {
            throw ConflictException()
        }

        if (innloggetBrukerHarAlleredeGodkjentPlan(oppfolgingsplan, innloggetAktoerId)) {
            throw ConflictException()
        }

        oppfolgingsplan = oppfolgingsplanDAO.populate(oppfolgingsplan)
        val narmesteleder: Optional<Naermesteleder> = narmesteLederConsumer.narmesteLeder(
            oppfolgingsplan.arbeidstaker.fnr,
            oppfolgingsplan.virksomhet.virksomhetsnummer
        )

        if (isLoggedInpersonLeaderAndOwnLeader(
                oppfolgingsplan,
                innloggetFnr,
                narmesteleder.get().naermesteLederFnr()
            )
        ) {
            LOG.info("TRACE: innlogget user attempting to godkjenne oppfolginsplan {}", oppfolgingsplanId)
            genererLederSinEgenPlan(oppfolgingsplan, gyldighetstidspunkt, delMedNav)
            godkjenningerDAO.deleteAllByOppfoelgingsdialogId(oppfolgingsplanId)
            sendGodkjentPlanTilAltinn(oppfolgingsplanId)
            LOG.info("TRACE: innlogget user godkjente oppfolginsplan successfully {}", oppfolgingsplanId)
        } else {
            val message = "Fulløring av oppfølgingplan feilet pga innlogget bruker ikke er egen leder"
            LOG.error(message)
            metrikk.tellHendelse("feil_godkjenn_plan_egen_leder")
            throw RuntimeException(message)
        }
    }

    @Transactional
    fun godkjennOppfolgingsplan(
        oppfolgingsplanId: Long,
        gyldighetstidspunkt: GyldighetstidspunktDTO?,
        innloggetFnr: String,
        tvungenGodkjenning: Boolean,
        delMedNav: Boolean
    ) {
        var oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)

        val innloggetAktoerId: String = pdlConsumer.aktorid(innloggetFnr)
        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(innloggetFnr, oppfolgingsplan)) {
            throw ForbiddenException("Ikke tilgang")
        }

        if (annenPartHarGjortEndringerImellomtiden(oppfolgingsplan, innloggetAktoerId)) {
            throw ConflictException()
        }

        if (innloggetBrukerHarAlleredeGodkjentPlan(oppfolgingsplan, innloggetAktoerId)) {
            throw ConflictException()
        }

        oppfolgingsplan = oppfolgingsplanDAO.populate(oppfolgingsplan)
        val erArbeidstaker: Boolean = erArbeidstakeren(oppfolgingsplan, innloggetAktoerId)
        var arbeidstakersFnr: String = oppfolgingsplan.arbeidstaker.fnr
        if (arbeidstakersFnr == null) {
            arbeidstakersFnr = pdlConsumer.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        }
        val virksomhetsnummer: String = oppfolgingsplan.virksomhet.virksomhetsnummer

        if (tvungenGodkjenning && erArbeidstaker) {
            val narmesteleder: Optional<Naermesteleder> =
                narmesteLederConsumer.narmesteLeder(arbeidstakersFnr, oppfolgingsplan.virksomhet.virksomhetsnummer)

            if (narmesteleder.isPresent() && narmesteleder.get().naermesteLederFnr.equals(innloggetFnr)) {
                LOG.info("TRACE: Arbeidstaker attempting to Tvangsgodkjenne oppfolginsplan {}", oppfolgingsplanId)
                genererTvungenPlan(oppfolgingsplan, gyldighetstidspunkt, delMedNav)
                godkjenningerDAO.deleteAllByOppfoelgingsdialogId(oppfolgingsplanId)
                sendGodkjentPlanTilAltinn(oppfolgingsplanId)
            } else {
                val message = "Tvangsgodkjenning av plan feilet fordi Arbeidstaker ikke er sin egen leder"
                LOG.error(message)
                metrikk.tellHendelse("feil_godkjenn_tvang_egen_leder")
                throw RuntimeException(message)
            }
        } else if (!erArbeidstaker && tvungenGodkjenning) {
            genererTvungenPlan(oppfolgingsplan, gyldighetstidspunkt, delMedNav)
            godkjenningerDAO.deleteAllByOppfoelgingsdialogId(oppfolgingsplanId)
            sendGodkjentPlanTilAltinn(oppfolgingsplanId)
        } else if (erGodkjentAvAnnenPart(oppfolgingsplanDAO.populate(oppfolgingsplan), innloggetAktoerId)) {
            genererNyPlan(oppfolgingsplan, innloggetAktoerId, delMedNav)
            godkjenningerDAO.deleteAllByOppfoelgingsdialogId(oppfolgingsplanId)
            sendGodkjentPlanTilAltinn(oppfolgingsplanId)
        } else {
            if (godkjenningRemoved(gyldighetstidspunkt, oppfolgingsplan) || godkjent(oppfolgingsplan)) {
                throw ConflictException()
            }
            godkjenningerDAO.create(
                Godkjenning()
                    .oppfoelgingsdialogId(oppfolgingsplanId)
                    .godkjent(true)
                    .delMedNav(delMedNav)
                    .godkjentAvAktoerId(innloggetAktoerId)
                    .gyldighetstidspunkt(
                        Gyldighetstidspunkt()
                            .fom(gyldighetstidspunkt.fom)
                            .tom(gyldighetstidspunkt.tom)
                            .evalueres(gyldighetstidspunkt.evalueres)
                    )
            )

            if (!erArbeidstaker) {
                esyfovarselService.sendVarselTilArbeidstaker(
                    SyfoplangodkjenningSyk,
                    oppfolgingsplan.arbeidstaker.fnr,
                    oppfolgingsplan.virksomhet.virksomhetsnummer
                )
            } else {
                narmesteLederConsumer.narmesteLeder(arbeidstakersFnr, virksomhetsnummer)
                    .ifPresent { naermesteleder ->
                        esyfovarselService.sendVarselTilNarmesteLeder(
                            SyfoplangodkjenningNl,
                            naermesteleder
                        )
                    }
            }
        }
        oppfolgingsplanDAO.sistEndretAv(oppfolgingsplanId, innloggetAktoerId)
    }

    private fun godkjent(oppfolgingsplan: Oppfolgingsplan): Boolean {
        return oppfolgingsplan.godkjentPlan.isPresent()
    }

    private fun godkjenningRemoved(
        gyldighetstidspunkt: Gyldighetstidspunkt?,
        oppfolgingsplan: Oppfolgingsplan
    ): Boolean {
        return gyldighetstidspunkt == null && oppfolgingsplan.godkjenninger.isEmpty()
    }

    private fun innloggetBrukerHarAlleredeGodkjentPlan(
        oppfolgingsplan: Oppfolgingsplan,
        innloggetAktoerId: String
    ): Boolean {
        return godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(oppfolgingsplan.id).stream()
            .anyMatch { godkjenning ->
                godkjenning.godkjent && godkjenning.godkjentAvAktoerId.equals(
                    innloggetAktoerId
                )
            }
    }

    private fun finnGodkjenning(oppfolgingsplan: Oppfolgingsplan): Godkjenning {
        return oppfolgingsplan.godkjenninger.stream()
            .filter { pGodkjenning -> pGodkjenning.godkjent }
            .findFirst().orElseThrow { RuntimeException("Fant ikke godkjenning") }
    }

    private fun rapporterMetrikkerForNyPlan(
        oppfolgingsplan: Oppfolgingsplan,
        erPlanTvungenGodkjent: Boolean,
        delMedNav: Boolean
    ) {
        if (erPlanTvungenGodkjent) {
            metrikk.tellHendelse("genererTvungenPlan")
        } else {
            metrikk.tellHendelse("genererNyPlan")
        }

        if (delMedNav) {
            metrikk.tellHendelse("del_plan_med_nav_ved_generer_godkjent_plan")
        }

        metrikk.tellHendelseMedAntall("tiltak", oppfolgingsplan.tiltakListe.size())
        metrikk.tellHendelseMedAntall("arbeidsoppgaver", oppfolgingsplan.arbeidsoppgaveListe.size())

        val antallArboppgGjennomforNormalt: Long = oppfolgingsplan.arbeidsoppgaveListe
            .stream()
            .filter { arbeidsoppgave -> KAN.name().equals(arbeidsoppgave.gjennomfoering.gjennomfoeringStatus) }
            .count()
        val antallArboppgGjennomforTilrettelegging: Long = oppfolgingsplan.arbeidsoppgaveListe
            .stream()
            .filter { arbeidsoppgave ->
                TILRETTELEGGING.name().equals(arbeidsoppgave.gjennomfoering.gjennomfoeringStatus)
            }
            .count()
        val antallArboppgGjennomforIkke: Long = oppfolgingsplan.arbeidsoppgaveListe
            .stream()
            .filter { arbeidsoppgave -> KAN_IKKE.name().equals(arbeidsoppgave.gjennomfoering.gjennomfoeringStatus) }
            .count()

        metrikk.tellHendelseMedAntall("arbeidsoppgaverGjennomforesNormalt", antallArboppgGjennomforNormalt)
        metrikk.tellHendelseMedAntall(
            "arbeidsoppgaverGjennomforesTilrettelegging",
            antallArboppgGjennomforTilrettelegging
        )
        metrikk.tellHendelseMedAntall("arbeidsoppgaverGjennomforesIkke", antallArboppgGjennomforIkke)

        val arbeidstakerAktoerId: String = oppfolgingsplan.arbeidstaker.aktoerId

        val antallArbboppgVurdertOgOpprettetAvAT: Long = oppfolgingsplan.arbeidsoppgaveListe
            .stream()
            .filter { arbeidsoppgave ->
                arbeidsoppgave.erVurdertAvSykmeldt && arbeidsoppgave.opprettetAvAktoerId.equals(
                    arbeidstakerAktoerId
                )
            }
            .count()
        val antallArbboppgVurdertOgOpprettetAvNL: Long = oppfolgingsplan.arbeidsoppgaveListe
            .stream()
            .filter { arbeidsoppgave ->
                arbeidsoppgave.erVurdertAvSykmeldt && !arbeidsoppgave.opprettetAvAktoerId.equals(
                    arbeidstakerAktoerId
                )
            }
            .count()
        val antallArbboppgIkkeVurdertOgOpprettetAvAT: Long = oppfolgingsplan.arbeidsoppgaveListe
            .stream()
            .filter { arbeidsoppgave ->
                !(arbeidsoppgave.erVurdertAvSykmeldt || arbeidsoppgave.opprettetAvAktoerId.equals(
                    arbeidstakerAktoerId
                ))
            }
            .count()
        metrikk.tellHendelseMedAntall("arbeidsoppgaverVurdertAvATOpprettetAvAT", antallArbboppgVurdertOgOpprettetAvAT)
        metrikk.tellHendelseMedAntall("arbeidsoppgaverVurdertAvATOpprettetAvNL", antallArbboppgVurdertOgOpprettetAvNL)
        metrikk.tellHendelseMedAntall(
            "arbeidsoppgaverIkkeVurdertAvATOpprettetAvNL",
            antallArbboppgIkkeVurdertOgOpprettetAvAT
        )

        val kommentarListe: List<Kommentar> = oppfolgingsplan.tiltakListe
            .stream()
            .flatMap { tiltak -> tiltak.kommentarer.stream() }
            .collect(Collectors.toList<T>())
        val antallKommentarerAT = kommentarListe
            .stream()
            .filter { kommentar: Kommentar -> kommentar.opprettetAvAktoerId.equals(arbeidstakerAktoerId) }
            .count()
        val antallKommentarerNL = kommentarListe
            .stream()
            .filter { kommentar: Kommentar -> !kommentar.opprettetAvAktoerId.equals(arbeidstakerAktoerId) }
            .count()
        metrikk.tellHendelseMedAntall("tiltakKommentarerFraAT", antallKommentarerAT)
        metrikk.tellHendelseMedAntall("tiltakKommentarerFraNL", antallKommentarerNL)
    }

    fun genererNyPlan(oppfolgingsplan: Oppfolgingsplan, innloggetAktoerId: String?, delMedNav: Boolean) {
        rapporterMetrikkerForNyPlan(oppfolgingsplan, false, delMedNav)

        val arbeidstakersFnr: String = pdlConsumer.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        val naermesteleder: Naermesteleder =
            narmesteLederConsumer.narmesteLeder(arbeidstakersFnr, oppfolgingsplan.virksomhet.virksomhetsnummer)
                .orElseThrow { RuntimeException("Fant ikke nærmeste leder") }
        val sykmeldtKontaktinfo: DigitalKontaktinfo = dkifConsumer.kontaktinformasjon(oppfolgingsplan.arbeidstaker.fnr)
        val sykmeldtnavn = brukerprofilService.hentNavnByAktoerId(oppfolgingsplan.arbeidstaker.aktoerId)
        val sykmeldtFnr: String = pdlConsumer.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        val virksomhetsnavn: String = eregConsumer.virksomhetsnavn(oppfolgingsplan.virksomhet.virksomhetsnummer)
        val xml: String = JAXB.marshallDialog(
            OppfoelgingsdialogXML()
                .withArbeidsgiverEpost(naermesteleder.epost)
                .withArbeidsgivernavn(naermesteleder.navn)
                .withArbeidsgiverOrgnr(oppfolgingsplan.virksomhet.virksomhetsnummer)
                .withVirksomhetsnavn(virksomhetsnavn)
                .withArbeidsgiverTlf(naermesteleder.mobil)
                .withEvalueres(tilMuntligDatoAarFormat(finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.evalueres))
                .withGyldigfra(tilMuntligDatoAarFormat(finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.fom))
                .withGyldigtil(tilMuntligDatoAarFormat(finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.tom))
                .withIkkeTattStillingTilArbeidsoppgaveXML(mapListe(
                    finnIkkeTattStillingTilArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    IkkeTattStillingTilArbeidsoppgaveXML()
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanIkkeGjennomfoeresArbeidsoppgaveXMLList(mapListe(
                    finnKanIkkeGjennomfoeresArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanIkkeGjennomfoeresArbeidsoppgaveXML()
                        .withBeskrivelse(arbeidsoppgave.gjennomfoering.kanIkkeBeskrivelse)
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanGjennomfoeresMedTilretteleggingArbeidsoppgaveXMLList(mapListe(
                    finnKanGjennomfoeresMedTilretteleggingArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanGjennomfoeresMedTilretteleggingArbeidsoppgaveXML()
                        .withMedHjelp(arbeidsoppgave.gjennomfoering.medHjelp)
                        .withMedMerTid(arbeidsoppgave.gjennomfoering.medMerTid)
                        .withPaaAnnetSted(arbeidsoppgave.gjennomfoering.paaAnnetSted)
                        .withBeskrivelse(arbeidsoppgave.gjennomfoering.kanBeskrivelse)
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanGjennomfoeresArbeidsoppgaveXMLList(mapListe(
                    finnKanGjennomfoeresArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanGjennomfoeresArbeidsoppgaveXML()
                        .withNavn(arbeidsoppgave.navn)
                })
                .withTiltak(mapListe(
                    oppfolgingsplan.tiltakListe
                ) { tiltak ->
                    TiltakXML()
                        .withNavn(tiltak.navn)
                        .withBeskrivelse(tiltak.beskrivelse)
                        .withBeskrivelseIkkeAktuelt(tiltak.beskrivelseIkkeAktuelt)
                        .withStatus(tiltak.status)
                        .withId(tiltak.id)
                        .withGjennomfoering(tiltak.gjennomfoering)
                        .withFom(
                            tilMuntligDatoAarFormat(
                                Optional.ofNullable<Any>(tiltak.fom)
                                    .orElse(finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.fom)
                            )
                        )
                        .withTom(
                            tilMuntligDatoAarFormat(
                                Optional.ofNullable<Any>(tiltak.tom)
                                    .orElse(finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.tom)
                            )
                        )
                        .withOpprettetAv(brukerprofilService.hentNavnByAktoerId(tiltak.opprettetAvAktoerId))
                })
                .withStillingListe(mapListe(
                    arbeidsforholdService.arbeidstakersStillingerForOrgnummer(
                        oppfolgingsplan.arbeidstaker.aktoerId,
                        finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.fom,
                        oppfolgingsplan.virksomhet.virksomhetsnummer
                    )
                ) { stilling ->
                    StillingXML()
                        .withYrke(stilling.yrke)
                        .withProsent(stilling.prosent)
                })
                .withSykmeldtFnr(sykmeldtFnr)
                .withSykmeldtNavn(sykmeldtnavn)
                .withSykmeldtTlf(sykmeldtKontaktinfo.getMobiltelefonnummer())
                .withSykmeldtEpost(sykmeldtKontaktinfo.getEpostadresse())
                .withVisAdvarsel(false)
                .withFotnote("Oppfølgningsplanen mellom " + sykmeldtnavn + " og " + naermesteleder.navn)
                .withOpprettetAv(
                    if (!erArbeidstakeren(
                            oppfolgingsplan,
                            innloggetAktoerId
                        )
                    ) sykmeldtnavn else naermesteleder.navn
                )
                .withOpprettetDato(tilMuntligDatoAarFormat(finnGodkjenning(oppfolgingsplan).godkjenningsTidspunkt.toLocalDate()))
                .withGodkjentAv(
                    if (erArbeidstakeren(
                            oppfolgingsplan,
                            innloggetAktoerId
                        )
                    ) sykmeldtnavn else naermesteleder.navn
                )
                .withGodkjentDato(tilMuntligDatoAarFormat(LocalDate.now()))
        )

        val dokumentUuid = UUID.randomUUID().toString()

        val skalDeleMedNav = delMedNav || oppfolgingsplan.godkjenninger.stream()
            .anyMatch { godkjenning -> godkjenning.delMedNav }

        godkjentplanDAO.create(
            GodkjentPlan()
                .oppfoelgingsdialogId(oppfolgingsplan.id)
                .deltMedNAV(skalDeleMedNav)
                .deltMedNAVTidspunkt(if ((skalDeleMedNav)) LocalDateTime.now() else null)
                .tvungenGodkjenning(false)
                .dokumentUuid(dokumentUuid)
                .gyldighetstidspunkt(
                    Gyldighetstidspunkt()
                        .fom(finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.fom)
                        .tom(finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.tom)
                        .evalueres(finnGodkjenning(oppfolgingsplan).gyldighetstidspunkt.evalueres)
                )
        )
        metrikk.tellAntallDagerSiden(oppfolgingsplan.opprettet, "opprettettilgodkjent")
        metrikk.tellAntallDagerSiden(finnGodkjenning(oppfolgingsplan).godkjenningsTidspunkt, "fragodkjenningtilplan")
        dokumentDAO.lagre(
            Dokument()
                .uuid(dokumentUuid)
                .pdf(tilPdf(xml))
                .xml(xml)
        )
    }

    fun genererTvungenPlan(
        oppfolgingsplan: Oppfolgingsplan,
        gyldighetstidspunkt: Gyldighetstidspunkt,
        delMedNav: Boolean
    ) {
        rapporterMetrikkerForNyPlan(oppfolgingsplan, true, delMedNav)

        val arbeidstakersFnr: String = pdlConsumer.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        val naermesteleder: Naermesteleder =
            narmesteLederConsumer.narmesteLeder(arbeidstakersFnr, oppfolgingsplan.virksomhet.virksomhetsnummer)
                .orElseThrow { RuntimeException("Fant ikke nærmeste leder") }
        val sykmeldtKontaktinfo: DigitalKontaktinfo = dkifConsumer.kontaktinformasjon(oppfolgingsplan.arbeidstaker.fnr)
        val sykmeldtnavn = brukerprofilService.hentNavnByAktoerId(oppfolgingsplan.arbeidstaker.aktoerId)
        val sykmeldtFnr: String = pdlConsumer.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        val virksomhetsnavn: String = eregConsumer.virksomhetsnavn(oppfolgingsplan.virksomhet.virksomhetsnummer)
        val xml: String = JAXB.marshallDialog(
            OppfoelgingsdialogXML()
                .withArbeidsgiverEpost(naermesteleder.epost)
                .withArbeidsgivernavn(naermesteleder.navn)
                .withArbeidsgiverOrgnr(oppfolgingsplan.virksomhet.virksomhetsnummer)
                .withVirksomhetsnavn(virksomhetsnavn)
                .withArbeidsgiverTlf(naermesteleder.mobil)
                .withEvalueres(tilMuntligDatoAarFormat(gyldighetstidspunkt.evalueres()))
                .withGyldigfra(tilMuntligDatoAarFormat(gyldighetstidspunkt.fom))
                .withGyldigtil(tilMuntligDatoAarFormat(gyldighetstidspunkt.tom))
                .withIkkeTattStillingTilArbeidsoppgaveXML(mapListe(
                    finnIkkeTattStillingTilArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    IkkeTattStillingTilArbeidsoppgaveXML()
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanIkkeGjennomfoeresArbeidsoppgaveXMLList(mapListe(
                    finnKanIkkeGjennomfoeresArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanIkkeGjennomfoeresArbeidsoppgaveXML()
                        .withBeskrivelse(arbeidsoppgave.gjennomfoering.kanIkkeBeskrivelse)
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanGjennomfoeresMedTilretteleggingArbeidsoppgaveXMLList(mapListe(
                    finnKanGjennomfoeresMedTilretteleggingArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanGjennomfoeresMedTilretteleggingArbeidsoppgaveXML()
                        .withMedHjelp(arbeidsoppgave.gjennomfoering.medHjelp)
                        .withMedMerTid(arbeidsoppgave.gjennomfoering.medMerTid)
                        .withPaaAnnetSted(arbeidsoppgave.gjennomfoering.paaAnnetSted)
                        .withBeskrivelse(arbeidsoppgave.gjennomfoering.kanBeskrivelse)
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanGjennomfoeresArbeidsoppgaveXMLList(mapListe(
                    finnKanGjennomfoeresArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanGjennomfoeresArbeidsoppgaveXML()
                        .withNavn(arbeidsoppgave.navn)
                })
                .withTiltak(mapListe(
                    oppfolgingsplan.tiltakListe
                ) { tiltak ->
                    TiltakXML()
                        .withNavn(tiltak.navn)
                        .withBeskrivelse(tiltak.beskrivelse)
                        .withBeskrivelseIkkeAktuelt(tiltak.beskrivelseIkkeAktuelt)
                        .withStatus(tiltak.status)
                        .withId(tiltak.id)
                        .withGjennomfoering(tiltak.gjennomfoering)
                        .withFom(
                            tilMuntligDatoAarFormat(
                                Optional.ofNullable<Any>(tiltak.fom).orElse(gyldighetstidspunkt.fom)
                            )
                        )
                        .withTom(
                            tilMuntligDatoAarFormat(
                                Optional.ofNullable<Any>(tiltak.tom).orElse(gyldighetstidspunkt.tom)
                            )
                        )
                        .withOpprettetAv(brukerprofilService.hentNavnByAktoerId(tiltak.opprettetAvAktoerId))
                })
                .withStillingListe(mapListe(
                    arbeidsforholdService.arbeidstakersStillingerForOrgnummer(
                        oppfolgingsplan.arbeidstaker.aktoerId,
                        gyldighetstidspunkt.fom,
                        oppfolgingsplan.virksomhet.virksomhetsnummer
                    )
                ) { stilling ->
                    StillingXML()
                        .withYrke(stilling.yrke)
                        .withProsent(stilling.prosent)
                })
                .withSykmeldtFnr(sykmeldtFnr)
                .withFotnote("Oppfølgningsplanen mellom " + sykmeldtnavn + " og " + naermesteleder.navn)
                .withSykmeldtNavn(sykmeldtnavn)
                .withSykmeldtTlf(sykmeldtKontaktinfo.getMobiltelefonnummer())
                .withSykmeldtEpost(sykmeldtKontaktinfo.getEpostadresse())
                .withVisAdvarsel(true)
                .withGodkjentAv(naermesteleder.navn)
                .withOpprettetAv(naermesteleder.navn)
                .withOpprettetDato(tilMuntligDatoAarFormat(LocalDate.now()))
                .withGodkjentDato(tilMuntligDatoAarFormat(LocalDate.now()))
        )
        val dokumentUuid = UUID.randomUUID().toString()
        godkjentplanDAO.create(
            GodkjentPlan()
                .oppfoelgingsdialogId(oppfolgingsplan.id)
                .deltMedNAV(delMedNav)
                .deltMedNAVTidspunkt(if (delMedNav) LocalDateTime.now() else null)
                .deltMedFastlege(false)
                .tvungenGodkjenning(true)
                .dokumentUuid(dokumentUuid)
                .gyldighetstidspunkt(
                    Gyldighetstidspunkt()
                        .fom(gyldighetstidspunkt.fom)
                        .tom(gyldighetstidspunkt.tom)
                        .evalueres(gyldighetstidspunkt.evalueres)
                )
        )

        dokumentDAO.lagre(
            Dokument()
                .uuid(dokumentUuid)
                .pdf(tilPdf(xml))
                .xml(xml)
        )
    }

    fun genererLederSinEgenPlan(
        oppfolgingsplan: Oppfolgingsplan, gyldighetstidspunkt: Gyldighetstidspunkt,
        delMedNav: Boolean
    ) {
        rapporterMetrikkerForNyPlan(oppfolgingsplan, true, delMedNav)

        val arbeidstakersFnr: String = pdlConsumer.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        val naermesteleder: Naermesteleder =
            narmesteLederConsumer.narmesteLeder(arbeidstakersFnr, oppfolgingsplan.virksomhet.virksomhetsnummer)
                .orElseThrow { RuntimeException("Fant ikke nærmeste leder") }
        val sykmeldtKontaktinfo: DigitalKontaktinfo = dkifConsumer.kontaktinformasjon(oppfolgingsplan.arbeidstaker.fnr)
        val sykmeldtnavn = brukerprofilService.hentNavnByAktoerId(oppfolgingsplan.arbeidstaker.aktoerId)
        val sykmeldtFnr: String = pdlConsumer.fnr(oppfolgingsplan.arbeidstaker.aktoerId)
        val virksomhetsnavn: String = eregConsumer.virksomhetsnavn(oppfolgingsplan.virksomhet.virksomhetsnummer)
        val xml: String = JAXB.marshallDialog(
            OppfoelgingsdialogXML()
                .withArbeidsgiverEpost(naermesteleder.epost)
                .withArbeidsgivernavn(naermesteleder.navn)
                .withArbeidsgiverOrgnr(oppfolgingsplan.virksomhet.virksomhetsnummer)
                .withVirksomhetsnavn(virksomhetsnavn)
                .withArbeidsgiverTlf(naermesteleder.mobil)
                .withEvalueres(tilMuntligDatoAarFormat(gyldighetstidspunkt.evalueres()))
                .withGyldigfra(tilMuntligDatoAarFormat(gyldighetstidspunkt.fom))
                .withGyldigtil(tilMuntligDatoAarFormat(gyldighetstidspunkt.tom))
                .withIkkeTattStillingTilArbeidsoppgaveXML(mapListe(
                    finnIkkeTattStillingTilArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    IkkeTattStillingTilArbeidsoppgaveXML()
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanIkkeGjennomfoeresArbeidsoppgaveXMLList(mapListe(
                    finnKanIkkeGjennomfoeresArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanIkkeGjennomfoeresArbeidsoppgaveXML()
                        .withBeskrivelse(arbeidsoppgave.gjennomfoering.kanIkkeBeskrivelse)
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanGjennomfoeresMedTilretteleggingArbeidsoppgaveXMLList(mapListe(
                    finnKanGjennomfoeresMedTilretteleggingArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanGjennomfoeresMedTilretteleggingArbeidsoppgaveXML()
                        .withMedHjelp(arbeidsoppgave.gjennomfoering.medHjelp)
                        .withMedMerTid(arbeidsoppgave.gjennomfoering.medMerTid)
                        .withPaaAnnetSted(arbeidsoppgave.gjennomfoering.paaAnnetSted)
                        .withBeskrivelse(arbeidsoppgave.gjennomfoering.kanBeskrivelse)
                        .withNavn(arbeidsoppgave.navn)
                })
                .withKanGjennomfoeresArbeidsoppgaveXMLList(mapListe(
                    finnKanGjennomfoeresArbeidsoppgaver(oppfolgingsplan.arbeidsoppgaveListe)
                ) { arbeidsoppgave ->
                    KanGjennomfoeresArbeidsoppgaveXML()
                        .withNavn(arbeidsoppgave.navn)
                })
                .withTiltak(mapListe(
                    oppfolgingsplan.tiltakListe
                ) { tiltak ->
                    TiltakXML()
                        .withNavn(tiltak.navn)
                        .withBeskrivelse(tiltak.beskrivelse)
                        .withBeskrivelseIkkeAktuelt(tiltak.beskrivelseIkkeAktuelt)
                        .withStatus(tiltak.status)
                        .withId(tiltak.id)
                        .withGjennomfoering(tiltak.gjennomfoering)
                        .withFom(
                            tilMuntligDatoAarFormat(
                                Optional.ofNullable<Any>(tiltak.fom).orElse(gyldighetstidspunkt.fom)
                            )
                        )
                        .withTom(
                            tilMuntligDatoAarFormat(
                                Optional.ofNullable<Any>(tiltak.tom).orElse(gyldighetstidspunkt.tom)
                            )
                        )
                        .withOpprettetAv(brukerprofilService.hentNavnByAktoerId(tiltak.opprettetAvAktoerId))
                })
                .withStillingListe(mapListe(
                    arbeidsforholdService.arbeidstakersStillingerForOrgnummer(
                        oppfolgingsplan.arbeidstaker.aktoerId,
                        gyldighetstidspunkt.fom,
                        oppfolgingsplan.virksomhet.virksomhetsnummer
                    )
                ) { stilling ->
                    StillingXML()
                        .withYrke(stilling.yrke)
                        .withProsent(stilling.prosent)
                })
                .withSykmeldtFnr(sykmeldtFnr)
                .withFotnote("Oppfølgningsplanen mellom " + sykmeldtnavn + " og " + naermesteleder.navn)
                .withSykmeldtNavn(sykmeldtnavn)
                .withSykmeldtTlf(sykmeldtKontaktinfo.getMobiltelefonnummer())
                .withSykmeldtEpost(sykmeldtKontaktinfo.getEpostadresse())
                .withVisAdvarsel(true)
                .withGodkjentAv(naermesteleder.navn)
                .withOpprettetAv(naermesteleder.navn)
                .withOpprettetDato(tilMuntligDatoAarFormat(LocalDate.now()))
                .withGodkjentDato(tilMuntligDatoAarFormat(LocalDate.now()))
        )
        val dokumentUuid = UUID.randomUUID().toString()
        godkjentplanDAO.create(
            GodkjentPlan()
                .oppfoelgingsdialogId(oppfolgingsplan.id)
                .deltMedNAV(delMedNav)
                .deltMedNAVTidspunkt(if (delMedNav) LocalDateTime.now() else null)
                .deltMedFastlege(false)
                .tvungenGodkjenning(false)
                .dokumentUuid(dokumentUuid)
                .gyldighetstidspunkt(
                    Gyldighetstidspunkt()
                        .fom(gyldighetstidspunkt.fom)
                        .tom(gyldighetstidspunkt.tom)
                        .evalueres(gyldighetstidspunkt.evalueres)
                )
        )

        dokumentDAO.lagre(
            Dokument()
                .uuid(dokumentUuid)
                .pdf(tilPdf(xml))
                .xml(xml)
        )
    }

    @Transactional
    fun avvisGodkjenning(oppfoelgingsdialogId: Long, innloggetFnr: String) {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfoelgingsdialogId)
        val innloggetAktoerId: String = pdlConsumer.aktorid(innloggetFnr)

        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(innloggetFnr, oppfolgingsplan)) {
            throw ForbiddenException("Ikke tilgang")
        }

        if (godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(oppfoelgingsdialogId).size() === 0) {
            throw ConflictException()
        }
        godkjenningerDAO.deleteAllByOppfoelgingsdialogId(oppfoelgingsdialogId)
        godkjenningerDAO.create(
            Godkjenning()
                .godkjent(false)
                .oppfoelgingsdialogId(oppfoelgingsdialogId)
                .godkjentAvAktoerId(innloggetAktoerId)
        )
        oppfolgingsplanDAO.nullstillSamtykke(oppfoelgingsdialogId)
        oppfolgingsplanDAO.sistEndretAv(oppfoelgingsdialogId, innloggetAktoerId)
    }

    private fun sendGodkjentPlanTilAltinn(oppfoelgingsdialogId: Long) {
        val ressursId = oppfoelgingsdialogId.toString()
        val sendOppfoelgingsdialog: AsynkOppgave =
            asynkOppgaveDAO.create(AsynkOppgave(OPPFOELGINGSDIALOG_SEND, ressursId))
        asynkOppgaveDAO.create(AsynkOppgave(OPPFOELGINGSDIALOG_ARKIVER, ressursId, sendOppfoelgingsdialog.id))
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GodkjenningService::class.java)
    }
}
