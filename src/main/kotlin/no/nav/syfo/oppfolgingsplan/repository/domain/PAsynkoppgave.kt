package no.nav.syfo.oppfolgingsplan.repository.domain

import java.time.LocalDateTime

data class PAsynkoppgave(
    var id: Long?,
    var opprettetTidspunkt: LocalDateTime?,
    var oppgavetype: String?,
    var avhengigAv: Long?,
    var antallForsoek: Int,
    var ressursId: String?
)
