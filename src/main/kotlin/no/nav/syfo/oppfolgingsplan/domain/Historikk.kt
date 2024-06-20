package no.nav.syfo.oppfolgingsplan.domain

import java.time.LocalDateTime

data class Historikk(
    val opprettetAv: String? = null,
    val tekst: String? = null,
    val tidspunkt: LocalDateTime? = null
)
