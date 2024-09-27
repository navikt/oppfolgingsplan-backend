package no.nav.syfo.aareg

import java.io.Serializable

data class Arbeidsavtale(
    var antallTimerPrUke: Double? = null,
    var arbeidstidsordning: String? = null,
    var beregnetAntallTimerPrUke: Double? = null,
    var bruksperiode: Bruksperiode? = null,
    var gyldighetsperiode: Gyldighetsperiode? = null,
    var sistLoennsendring: String? = null,
    var sistStillingsendring: String? = null,
    var sporingsinformasjon: Sporingsinformasjon? = null,
    var stillingsprosent: Double,
    var yrke: String
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
