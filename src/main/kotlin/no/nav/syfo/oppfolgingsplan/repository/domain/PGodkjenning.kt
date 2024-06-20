package no.nav.syfo.oppfolgingsplan.repository.domain

import no.nav.syfo.oppfolgingsplan.domain.GodkjenningDTO
import no.nav.syfo.oppfolgingsplan.domain.GyldighetstidspunktDTO
import java.time.LocalDateTime

data class PGodkjenning(
    var id: Long,
    var oppfoelgingsdialogId: Long,
    var created: LocalDateTime,
    var aktoerId: String?,
    var beskrivelse: String?,
    var godkjent: Boolean,
    var delMedNav: Boolean,
    var fom: LocalDateTime?,
    var tom: LocalDateTime?,
    var evalueres: LocalDateTime?
)

fun PGodkjenning.toGodkjenningDTO(): GodkjenningDTO {
    return GodkjenningDTO(
        id = this.id,
        oppfoelgingsdialogId = this.oppfoelgingsdialogId,
        godkjent = this.godkjent,
        delMedNav = this.delMedNav,
        godkjentAvAktoerId = this.aktoerId,
        beskrivelse = this.beskrivelse,
        godkjenningsTidspunkt = this.created,
        gyldighetstidspunkt = GyldighetstidspunktDTO(
            fom = this.fom?.toLocalDate(),
            tom = this.tom?.toLocalDate(),
            evalueres = this.evalueres?.toLocalDate()
        )
    )
}

fun List<PGodkjenning>.toGodkjenningDTOList(): List<GodkjenningDTO> {
    return this.map { it.toGodkjenningDTO() }
}
