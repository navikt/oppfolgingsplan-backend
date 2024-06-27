package no.nav.syfo.oppfolgingsplan.mapper

import no.nav.syfo.oppfolgingsplan.domain.GodkjentPlanDTO
import no.nav.syfo.oppfolgingsplan.domain.OppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplan.domain.rs.RSGodkjentPlan
import no.nav.syfo.oppfolgingsplan.domain.rs.RSGyldighetstidspunkt
import no.nav.syfo.oppfolgingsplan.domain.rs.RSOppfoelgingsdialog
import no.nav.syfo.oppfolgingsplan.domain.rs.RSVirksomhet
import java.time.LocalDate

fun godkjentplan2rs(godkjentPlan: GodkjentPlanDTO): RSGodkjentPlan {
    return RSGodkjentPlan(
        deltMedNAV = godkjentPlan.deltMedNAV,
        deltMedNAVTidspunkt = godkjentPlan.deltMedNAVTidspunkt,
        deltMedFastlege = godkjentPlan.deltMedFastlege,
        deltMedFastlegeTidspunkt = godkjentPlan.deltMedFastlegeTidspunkt,
        dokumentUuid = godkjentPlan.dokumentUuid,
        opprettetTidspunkt = godkjentPlan.opprettetTidspunkt,
        tvungenGodkjenning = godkjentPlan.tvungenGodkjenning,
        gyldighetstidspunkt = RSGyldighetstidspunkt(
            fom = godkjentPlan.gyldighetstidspunkt.fom,
            tom = godkjentPlan.gyldighetstidspunkt.tom,
            evalueres = godkjentPlan.gyldighetstidspunkt.evalueres
        )
    )
}

fun status2rs(oppfoelgingsdialog: OppfolgingsplanDTO): String {
    if (oppfoelgingsdialog.godkjentPlan == null) {
        return "UNDER_ARBEID"
    }

    return when {
        oppfoelgingsdialog.godkjentPlan.avbruttPlan != null -> {
            "AVBRUTT"
        }

        oppfoelgingsdialog.godkjentPlan.gyldighetstidspunkt.tom?.isBefore(LocalDate.now()) == true -> {
            "UTDATERT"
        }

        else -> "AKTIV"
    }
}

fun oppfoelgingsdialog2rs(oppfoelgingsdialog: OppfolgingsplanDTO): RSOppfoelgingsdialog {
    return RSOppfoelgingsdialog(
        id = oppfoelgingsdialog.id,
        uuid = oppfoelgingsdialog.uuid,
        sistEndretAvAktoerId = oppfoelgingsdialog.sistEndretAvAktoerId,
        sistEndretDato = oppfoelgingsdialog.sistEndretDato,
        status = status2rs(oppfoelgingsdialog),
        virksomhet = RSVirksomhet(
            virksomhetsnummer = oppfoelgingsdialog.virksomhet.virksomhetsnummer,
            navn = oppfoelgingsdialog.virksomhet.navn
        ),
        godkjentPlan = oppfoelgingsdialog.godkjentPlan?.let { godkjentplan2rs(it) }
    )
}
