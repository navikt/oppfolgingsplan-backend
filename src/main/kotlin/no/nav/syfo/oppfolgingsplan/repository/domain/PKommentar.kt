package no.nav.syfo.oppfolgingsplan.repository.domain

import no.nav.syfo.oppfolgingsplan.domain.KommentarDTO
import java.time.LocalDateTime

data class PKommentar(
    var id: Long,
    var tiltakId: Long,
    var tekst: String?,
    var sistEndretAvAktoerId: String?,
    var sistEndretDato: LocalDateTime?,
    var opprettetAvAktoerId: String?,
    var opprettetDato: LocalDateTime?
)

fun PKommentar.toKommentarDTO(): KommentarDTO {
    return KommentarDTO(
        id = this.id,
        tiltakId = this.tiltakId,
        tekst = this.tekst,
        sistEndretAvAktoerId = this.sistEndretAvAktoerId,
        sistEndretDato = this.sistEndretDato,
        opprettetAvAktoerId = this.opprettetAvAktoerId,
        opprettetDato = this.opprettetDato
    )
}
