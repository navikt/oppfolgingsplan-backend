package no.nav.syfo.oppfolgingsplan.util

import no.nav.syfo.oppfolgingsplan.domain.ArbeidsoppgaveDTO
import no.nav.syfo.oppfolgingsplan.domain.GjennomfoeringDTO.KanGjennomfoeres.KAN
import no.nav.syfo.oppfolgingsplan.domain.GjennomfoeringDTO.KanGjennomfoeres.KAN_IKKE
import no.nav.syfo.oppfolgingsplan.domain.GjennomfoeringDTO.KanGjennomfoeres.TILRETTELEGGING
import no.nav.syfo.oppfolgingsplan.domain.GodkjenningDTO
import no.nav.syfo.oppfolgingsplan.domain.OppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplan.domain.TiltakDTO

fun fjernEldsteGodkjenning(godkjenninger: List<GodkjenningDTO>): List<GodkjenningDTO> {
    return if (godkjenninger.size <= 1) {
        emptyList()
    } else {
        godkjenninger.sortedBy { it.godkjenningsTidspunkt }.drop(1)
    }
}

fun finnIkkeTattStillingTilArbeidsoppgaver(arbeidsoppgaveListe: List<ArbeidsoppgaveDTO>): List<ArbeidsoppgaveDTO> {
    return arbeidsoppgaveListe.filter { !it.erVurdertAvSykmeldt }
}

fun finnKanGjennomfoeresArbeidsoppgaver(arbeidsoppgaveListe: List<ArbeidsoppgaveDTO>): List<ArbeidsoppgaveDTO> {
    return arbeidsoppgaveListe.filter { KAN.name == it.gjennomfoering?.gjennomfoeringStatus?.name }
}

fun finnKanIkkeGjennomfoeresArbeidsoppgaver(arbeidsoppgaveListe: List<ArbeidsoppgaveDTO>): List<ArbeidsoppgaveDTO> {
    return arbeidsoppgaveListe.filter { KAN_IKKE.name == it.gjennomfoering?.gjennomfoeringStatus?.name }
}

fun finnKanGjennomfoeresMedTilrettelegging(arbeidsoppgaveListe: List<ArbeidsoppgaveDTO>): List<ArbeidsoppgaveDTO> {
    return arbeidsoppgaveListe.filter { TILRETTELEGGING.name == it.gjennomfoering?.gjennomfoeringStatus?.name }
}

fun erGodkjentAvAnnenPart(oppfolgingsplan: OppfolgingsplanDTO, aktoerId: String): Boolean {
    return if (erArbeidstakeren(oppfolgingsplan, aktoerId)) {
        harArbeidsgiverGodkjentPlan(oppfolgingsplan)
    } else {
        harArbeidstakerGodkjentPlan(oppfolgingsplan)
    }
}

fun annenPartHarGjortEndringerImellomtiden(oppfolgingsplan: OppfolgingsplanDTO, innloggetAktoerId: String): Boolean {
    return if (erArbeidstakeren(oppfolgingsplan, innloggetAktoerId)) {
        oppfolgingsplan.sistEndretArbeidsgiver != null &&
            oppfolgingsplan.arbeidstaker.sistAksessert != null &&
            oppfolgingsplan.arbeidstaker.sistAksessert.isBefore(
                oppfolgingsplan.sistEndretArbeidsgiver
            )
    } else {
        oppfolgingsplan.sistEndretSykmeldt != null &&
            oppfolgingsplan.arbeidsgiver.sistAksessert != null &&
            oppfolgingsplan.arbeidsgiver.sistAksessert.isBefore(
                oppfolgingsplan.sistEndretSykmeldt
            )
    }
}

fun sisteGodkjenningAvAnnenPart(oppfolgingsplan: OppfolgingsplanDTO, innloggetAktoerId: String): GodkjenningDTO {
    return if (erArbeidstakeren(oppfolgingsplan, innloggetAktoerId)) {
        oppfolgingsplan.godkjenninger.stream()
            .filter { godkjenning -> godkjenning.godkjentAvAktoerId == innloggetAktoerId }
            .sorted { o1, o2 -> o2.godkjenningsTidspunkt!!.compareTo(o1.godkjenningsTidspunkt) }.findFirst().get()
    } else {
        oppfolgingsplan.godkjenninger.stream()
            .filter { godkjenning -> godkjenning.godkjentAvAktoerId != innloggetAktoerId }
            .sorted { o1, o2 -> o2.godkjenningsTidspunkt!!.compareTo(o1.godkjenningsTidspunkt) }.findFirst().get()
    }
}

fun harArbeidsgiverGodkjentPlan(oppfolgingsplan: OppfolgingsplanDTO): Boolean {
    return oppfolgingsplan.godkjenninger.any { godkjenning ->
        !erArbeidstakeren(oppfolgingsplan, godkjenning.godkjentAvAktoerId) && godkjenning.godkjent
    }
}

fun harArbeidstakerGodkjentPlan(oppfolgingsplan: OppfolgingsplanDTO): Boolean {
    return oppfolgingsplan.godkjenninger.any { godkjenning ->
        erArbeidstakeren(oppfolgingsplan, godkjenning.godkjentAvAktoerId) && godkjenning.godkjent
    }
}

fun erArbeidstakeren(oppfolgingsplan: OppfolgingsplanDTO, aktoerId: String?): Boolean {
    return aktoerId == oppfolgingsplan.arbeidstaker.aktoerId
}

fun erArbeidsgiveren(oppfolgingsplan: OppfolgingsplanDTO, aktoerId: String?): Boolean {
    return !erArbeidstakeren(oppfolgingsplan, aktoerId)
}

fun isLoggedInpersonLeaderAndOwnLeader(
    oppfolgingsplan: OppfolgingsplanDTO,
    loggedInPersonFnr: String,
    leaderFnr: String
): Boolean {
    return oppfolgingsplan.arbeidstaker.fnr == leaderFnr && loggedInPersonFnr == leaderFnr
}

fun eksisterendeTiltakHoererTilDialog(tiltakId: Long?, tiltakListe: List<TiltakDTO>): Boolean {
    return tiltakId == null || tiltakListe.any { tiltak -> tiltak.id == tiltakId }
}

fun eksisterendeArbeidsoppgaveHoererTilDialog(
    arbeidsoppgaveId: Long?,
    arbeidsoppgaveListe: List<ArbeidsoppgaveDTO>
): Boolean {
    return arbeidsoppgaveId == null ||
        arbeidsoppgaveListe.any { arbeidsoppgave -> arbeidsoppgave.id == arbeidsoppgaveId }
}

fun kanEndreElement(innloggetAktoerId: String, arbeidstakerAktoerId: String, opprettetAvAktoerId: String): Boolean {
    return when {
        opprettetAvAktoerId == innloggetAktoerId -> {
            true
        }
        // Hvis tidligere nærmeste leder har opprettet elementet, skal ny nærmeste leder kunne endre det
        innloggetAktoerId != arbeidstakerAktoerId && opprettetAvAktoerId != arbeidstakerAktoerId -> {
            true
        }
        else -> false
    }
}
