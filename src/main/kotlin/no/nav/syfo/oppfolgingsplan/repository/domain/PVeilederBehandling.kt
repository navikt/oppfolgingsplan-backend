package no.nav.syfo.oppfolgingsplan.repository.domain

import java.time.LocalDateTime

data class PVeilederBehandling(
    var oppgaveId: Long,
    var oppgaveUUID: String?,
    var godkjentplanId: Long?,
    var tildeltIdent: String?,
    var tildeltEnhet: String?,
    var opprettetDato: LocalDateTime?,
    var sistEndret: LocalDateTime?,
    var sistEndretAv: String?,
    var status: String?
)
