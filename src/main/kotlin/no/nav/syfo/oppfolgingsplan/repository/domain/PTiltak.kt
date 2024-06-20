package no.nav.syfo.oppfolgingsplan.repository.domain

import no.nav.syfo.oppfolgingsplan.domain.KommentarDTO
import no.nav.syfo.oppfolgingsplan.domain.TiltakDTO
import java.time.LocalDateTime

data class PTiltak(
    var id: Long,
    var oppfoelgingsdialogId: Long,
    var navn: String?,
    var fom: LocalDateTime?,
    var tom: LocalDateTime?,
    var beskrivelse: String?,
    var opprettetAvAktoerId: String?,
    var sistEndretAvAktoerId: String?,
    var sistEndretDato: LocalDateTime?,
    var opprettetDato: LocalDateTime?,
    var status: String?,
    var gjennomfoering: String?,
    var beskrivelseIkkeAktuelt: String?
)

fun PTiltak.toTiltakDTO(kommentarer: List<KommentarDTO>): TiltakDTO {
    return TiltakDTO(
        id = this.id,
        oppfoelgingsdialogId = this.oppfoelgingsdialogId,
        navn = this.navn,
        fom = this.fom?.toLocalDate(),
        tom = this.tom?.toLocalDate(),
        beskrivelse = this.beskrivelse,
        beskrivelseIkkeAktuelt = this.beskrivelseIkkeAktuelt,
        sistEndretAvAktoerId = this.sistEndretAvAktoerId,
        sistEndretDato = this.sistEndretDato,
        opprettetAvAktoerId = this.opprettetAvAktoerId,
        opprettetDato = this.opprettetDato,
        status = this.status,
        gjennomfoering = this.gjennomfoering,
        kommentarer = kommentarer
    )
}
