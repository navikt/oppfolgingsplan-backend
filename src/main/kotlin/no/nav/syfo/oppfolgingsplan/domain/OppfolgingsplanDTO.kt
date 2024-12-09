package no.nav.syfo.oppfolgingsplan.domain

import no.nav.syfo.aareg.model.Stilling
import java.time.LocalDate
import java.time.LocalDateTime

data class OppfolgingsplanDTO(
    val id: Long,
    val uuid: String,
    val opprettet: LocalDateTime,
    val sistEndretAvAktoerId: String?,
    val sistEndretAvFnr: String,
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
    var id: Long? = null,
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
    val id: Long?,
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
    val navn: String = " ",
    val fnr: String?,
    val epost: String? = null,
    val tlf: String? = null,
    val samtykke: Boolean? = null,
    val evaluering: EvalueringDTO? = null,
    var stillinger: List<Stilling> = ArrayList(),
    var sistInnlogget: LocalDateTime? = null,
    var sisteEndring: LocalDateTime? = null,
    var sistAksessert: LocalDateTime? = null,
    val aktoerId: String? = null
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

data class Arbeidsgiver(
    val narmesteLeder: NarmesteLederDTO,
)

data class NarmesteLederDTO(
    val virksomhetsnummer: String? = null,
    val erAktiv: Boolean? = null,
    val aktivFom: LocalDate? = null,
    val aktivTom: LocalDate? = null,
    val navn: String = "",
    val fnr: String? = null,
    val epost: String? = null,
    val tlf: String? = null,
    val sistInnlogget: LocalDateTime? = null,
    val samtykke: Boolean? = null,
    val evaluering: EvalueringDTO? = null,
)

data class EvalueringDTO(
    val effekt: String? = null,
    val hvorfor: String? = null,
    val videre: String? = null,
    val interneaktiviteter: Boolean = false,
    val ekstratid: Boolean? = null,
    val bistand: Boolean? = null,
    val ingen: Boolean? = null,
)
