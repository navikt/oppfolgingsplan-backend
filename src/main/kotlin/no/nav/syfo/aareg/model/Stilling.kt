package no.nav.syfo.aareg.model

import java.math.BigDecimal
import java.time.LocalDate

data class Stilling(
    var yrke: String?,
    var prosent: BigDecimal?,
    var fom: LocalDate? = null,
    var tom: LocalDate? = null,
    var orgnummer: String? = null
)
