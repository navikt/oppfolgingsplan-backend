package no.nav.syfo.oppfolgingsplan.domain

import no.nav.syfo.aareg.model.Stilling
import java.time.LocalDate
import java.time.LocalDateTime

data class BrukerOppfolgingsplan(
    val id: Long,
    val sistEndretDato: LocalDateTime?,
    val opprettetDato: LocalDate,
    val status: Status,
    val virksomhet: VirksomhetDTO,
    val godkjentPlan: GodkjentPlanDTO? = null,
    val godkjenninger: List<GodkjenningDTO> = ArrayList(),
    val arbeidsoppgaveListe: List<ArbeidsoppgaveDTO> = ArrayList(),
    val tiltakListe: List<TiltakDTO> = ArrayList(),
    var avbruttPlanListe: List<AvbruttplanDTO> = ArrayList(),
    val arbeidsgiver: Arbeidsgiver,
    val arbeidstaker: PersonDTO,
    val sistEndretAv: PersonDTO,
)

fun OppfolgingsplanDTO.toBrukerOppfolgingsplan() =
    BrukerOppfolgingsplan(
        id = id,
        virksomhet = virksomhet,
        arbeidsoppgaveListe = arbeidsoppgaveListe,
        tiltakListe = tiltakListe,
        godkjenninger = godkjenninger,
        sistEndretAv = PersonDTO(
            fnr = sistEndretAvFnr,
        ),
        sistEndretDato = sistEndretDato,
        godkjentPlan = godkjentPlan,
        status = getStatus(),
        opprettetDato = LocalDate.from(opprettet),
        arbeidstaker = PersonDTO(
            fnr = arbeidstaker.fnr,
            sistInnlogget = arbeidstaker.sistInnlogget,
            evaluering = if (godkjentPlan != null) EvalueringDTO() else null,
            samtykke = arbeidstaker.samtykke
        ),
        arbeidsgiver = Arbeidsgiver(
            narmesteLeder = NarmesteLederDTO(
                sistInnlogget = arbeidsgiver.sistInnlogget,
                evaluering = if (godkjentPlan != null) EvalueringDTO() else null,
                samtykke = arbeidsgiver.samtykke
            )
        )
    )

fun BrukerOppfolgingsplan.populerPlanerMedAvbruttPlanListe(planer: List<BrukerOppfolgingsplan>) {
    avbruttPlanListe = planer.filter {
        it.arbeidstaker.fnr == arbeidstaker.fnr &&
            it.virksomhet.virksomhetsnummer == virksomhet.virksomhetsnummer &&
            it.godkjentPlan != null &&
            it.godkjentPlan.avbruttPlan != null &&
            it.opprettetDato.isBefore(opprettetDato)
    }
        .sortedByDescending { brukerOppfolgingsplan -> brukerOppfolgingsplan.godkjentPlan?.avbruttPlan?.tidspunkt }
        .map {
            it.godkjentPlan!!.avbruttPlan!!.id = it.id
            it.godkjentPlan.avbruttPlan!!
        }
}

fun BrukerOppfolgingsplan.populerArbeidstakersStillinger(arbeidsforhold: List<Stilling>) {
    arbeidstaker.stillinger = arbeidsforhold
        .filter { stilling ->
            stilling.fom?.isBefore(opprettetDato) == true && (
                stilling.tom == null || stilling.tom?.isAfter(
                    opprettetDato
                ) == true
                )
        }
        .filter { stilling -> stilling.orgnummer == virksomhet.virksomhetsnummer }
        .map { stilling -> Stilling(stilling.yrke, stilling.prosent) }
}
