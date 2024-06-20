package no.nav.syfo.oppfolgingsplan.repository.domain

import no.nav.syfo.oppfolgingsplan.domain.AvbruttplanDTO
import no.nav.syfo.oppfolgingsplan.domain.GodkjentPlanDTO
import no.nav.syfo.oppfolgingsplan.domain.GyldighetstidspunktDTO
import java.time.LocalDateTime

data class PGodkjentPlan(
    var id: Long,
    var oppfoelgingsdialogId: Long,
    var dokumentUuid: String?,
    var sakId: String?,
    var journalpostId: String?,
    var tildeltEnhet: String?,
    var deltMedNav: Boolean,
    var deltMedFastlege: Boolean,
    var tvungenGodkjenning: Boolean,
    var avbruttTidspunkt: LocalDateTime?,
    var avbruttAv: String?,
    var fom: LocalDateTime?,
    var tom: LocalDateTime?,
    var evalueres: LocalDateTime?,
    var deltMedNavTidspunkt: LocalDateTime?,
    var deltMedFastlegeTidspunkt: LocalDateTime?,
    var created: LocalDateTime
)

fun PGodkjentPlan.toGodkjentPlanDTO(): GodkjentPlanDTO {
    return GodkjentPlanDTO(
        id = this.id,
        oppfoelgingsdialogId = this.oppfoelgingsdialogId,
        opprettetTidspunkt = this.created,
        gyldighetstidspunkt = GyldighetstidspunktDTO(
            fom = this.fom?.toLocalDate(),
            tom = this.tom?.toLocalDate(),
            evalueres = this.evalueres?.toLocalDate()
        ),
        tvungenGodkjenning = this.tvungenGodkjenning,
        deltMedNAVTidspunkt = this.deltMedNavTidspunkt,
        deltMedNAV = this.deltMedNav,
        deltMedFastlege = this.deltMedFastlege,
        deltMedFastlegeTidspunkt = this.deltMedFastlegeTidspunkt,
        dokumentUuid = this.dokumentUuid,
        avbruttPlan = AvbruttplanDTO(
            avAktoerId = this.avbruttAv,
            tidspunkt = this.avbruttTidspunkt,
            oppfoelgingsdialogId = this.oppfoelgingsdialogId
        ),
        sakId = this.sakId,
        journalpostId = this.journalpostId,
        tildeltEnhet = this.tildeltEnhet,
        dokument = null
    )
}

fun List<PGodkjentPlan>.toGodkjentPlanDTOList(): List<GodkjentPlanDTO> {
    return this.map { it.toGodkjentPlanDTO() }
}
