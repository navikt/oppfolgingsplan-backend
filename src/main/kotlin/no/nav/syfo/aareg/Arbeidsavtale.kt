package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
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
) : Serializable
