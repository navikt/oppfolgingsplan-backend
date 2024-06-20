package no.nav.syfo.oppfolgingsplan.domain.rs

import java.time.LocalDateTime

data class RSPerson(
    var navn: String? = null,
    var aktoerId: String? = null,
    var fnr: String? = null,
    var epost: String? = null,
    var tlf: String? = null,
    var sistInnlogget: LocalDateTime? = null,
    var samtykke: Boolean? = null
)
