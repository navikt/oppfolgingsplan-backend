package no.nav.syfo.oppfolgingsplan.repository.domain

import no.nav.syfo.oppfolgingsplan.domain.ArbeidsoppgaveDTO
import no.nav.syfo.oppfolgingsplan.domain.GjennomfoeringDTO
import java.time.LocalDateTime

data class PArbeidsoppgave(
    var id: Long,
    var oppfoelgingsdialogId: Long,
    var navn: String?,
    var erVurdertAvSykmeldt: Boolean,
    var opprettetAvAktoerId: String?,
    var sistEndretAvAktoerId: String?,
    var sistEndretDato: LocalDateTime?,
    var opprettetDato: LocalDateTime,
    var gjennomfoeringStatus: String?,
    var paaAnnetSted: Boolean?,
    var medMerTid: Boolean?,
    var medHjelp: Boolean?,
    var kanBeskrivelse: String?,
    var kanIkkeBeskrivelse: String?
)

fun PArbeidsoppgave.toArbeidsoppgaveDTO(): ArbeidsoppgaveDTO {
    return ArbeidsoppgaveDTO(
        id = this.id,
        oppfoelgingsdialogId = this.oppfoelgingsdialogId,
        navn = this.navn,
        erVurdertAvSykmeldt = this.erVurdertAvSykmeldt,
        gjennomfoering = GjennomfoeringDTO(
            gjennomfoeringStatus = this.gjennomfoeringStatus?.let { GjennomfoeringDTO.KanGjennomfoeres.valueOf(it) },
            kanIkkeBeskrivelse = this.kanIkkeBeskrivelse,
            kanBeskrivelse = this.kanBeskrivelse,
            medMerTid = this.medMerTid,
            medHjelp = this.medHjelp,
            paaAnnetSted = this.paaAnnetSted,
        ),
        sistEndretAvAktoerId = this.sistEndretAvAktoerId,
        sistEndretDato = this.sistEndretDato,
        opprettetAvAktoerId = this.opprettetAvAktoerId,
        opprettetDato = this.opprettetDato
    )
}

fun List<PArbeidsoppgave>.toArbeidsoppgaveDTOList(): List<ArbeidsoppgaveDTO> {
    return this.map { it.toArbeidsoppgaveDTO() }
}
