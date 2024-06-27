package no.nav.syfo.oppfolgingsplan.domain.rs

import java.time.LocalDate

data class RSGyldighetstidspunkt(
    var fom: LocalDate? = null,
    var tom: LocalDate? = null,
    var evalueres: LocalDate? = null
)
