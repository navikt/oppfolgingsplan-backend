package no.nav.syfo.oppfolgingsplan.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class OppfolgingsplanDTO(
    val id: Long,
    val uuid: String,
    val opprettet: LocalDateTime,
    val sistEndretAvAktoerId: String?,
    val sistEndretAvFnr: String?,
    val opprettetAvAktoerId: String?,
    val opprettetAvFnr: String?,
    val sistEndretDato: LocalDateTime?,
    val sistEndretArbeidsgiver: LocalDateTime?,
    val sistEndretSykmeldt: LocalDateTime?,
    val status: String?,
    val virksomhet: VirksomhetDTO,
    val godkjentPlan: GodkjentPlanDTO?,
    val godkjenninger: List<GodkjenningDTO>,
    val arbeidsoppgaveListe: List<ArbeidsoppgaveDTO>,
    val tiltakListe: List<TiltakDTO>,
    val arbeidsgiver: PersonDTO,
    val arbeidstaker: PersonDTO
)

data class VirksomhetDTO(
    val navn: String?,
    val virksomhetsnummer: String
)

data class GodkjentPlanDTO(
    val id: Long,
    val oppfoelgingsdialogId: Long,
    val opprettetTidspunkt: LocalDateTime,
    val gyldighetstidspunkt: GyldighetstidspunktDTO,
    val tvungenGodkjenning: Boolean,
    val deltMedNAVTidspunkt: LocalDateTime?,
    val deltMedNAV: Boolean,
    val deltMedFastlege: Boolean,
    val deltMedFastlegeTidspunkt: LocalDateTime?,
    val dokumentUuid: String?,
    val avbruttPlan: AvbruttplanDTO?,
    val sakId: String?,
    val journalpostId: String?,
    val tildeltEnhet: String?,
    val dokument: ByteArray?
)

data class GyldighetstidspunktDTO(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val evalueres: LocalDate?
)

data class AvbruttplanDTO(
    val avAktoerId: String?,
    val tidspunkt: LocalDateTime?,
    val oppfoelgingsdialogId: Long?
)

data class GodkjenningDTO(
    val id: Long,
    val oppfoelgingsdialogId: Long,
    val godkjent: Boolean,
    val delMedNav: Boolean,
    val godkjentAvAktoerId: String?,
    val beskrivelse: String?,
    val godkjenningsTidspunkt: LocalDateTime?,
    val gyldighetstidspunkt: GyldighetstidspunktDTO?
)

data class ArbeidsoppgaveDTO(
    val id: Long,
    val oppfoelgingsdialogId: Long,
    val navn: String?,
    val erVurdertAvSykmeldt: Boolean,
    val gjennomfoering: GjennomfoeringDTO?,
    val sistEndretAvAktoerId: String?,
    val sistEndretDato: LocalDateTime?,
    val opprettetAvAktoerId: String?,
    val opprettetDato: LocalDateTime
)

data class GjennomfoeringDTO(
    val gjennomfoeringStatus: KanGjennomfoeres?,
    val paaAnnetSted: Boolean?,
    val medMerTid: Boolean?,
    val medHjelp: Boolean?,
    val kanBeskrivelse: String?,
    val kanIkkeBeskrivelse: String?,
) {
    enum class KanGjennomfoeres {
        KAN,
        KAN_IKKE,
        TILRETTELEGGING
    }
}

data class TiltakDTO(
    val id: Long,
    val oppfoelgingsdialogId: Long,
    val navn: String?,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val beskrivelse: String?,
    val status: String?,
    val gjennomfoering: String?,
    val beskrivelseIkkeAktuelt: String?,
    val kommentarer: List<KommentarDTO>?,
    val opprettetDato: LocalDateTime?,
    val sistEndretDato: LocalDateTime?,
    val opprettetAvAktoerId: String?,
    val sistEndretAvAktoerId: String?
)

data class KommentarDTO(
    val id: Long,
    val tiltakId: Long,
    val tekst: String?,
    val sistEndretAvAktoerId: String?,
    val sistEndretDato: LocalDateTime?,
    val opprettetAvAktoerId: String?,
    val opprettetDato: LocalDateTime?
)

data class PersonDTO(
    val navn: String?,
    val aktoerId: String?,
    val fnr: String?,
    val epost: String?,
    val tlf: String?,
    val sistInnlogget: LocalDateTime?,
    val sisteEndring: LocalDateTime?,
    val sistAksessert: LocalDateTime?,
    val samtykke: Boolean?
)

fun OppfolgingsplanDTO.getStatus(): Status {
    return when {
        godkjentPlan?.avbruttPlan != null -> Status.AVBRUTT
        godkjentPlan?.gyldighetstidspunkt?.tom?.isBefore(LocalDate.now()) == true -> Status.UTDATERT
        godkjentPlan != null -> Status.AKTIV
        else -> Status.UNDER_ARBEID
    }
}
enum class Status {
    AVBRUTT,
    UTDATERT,
    AKTIV,
    UNDER_ARBEID
}
