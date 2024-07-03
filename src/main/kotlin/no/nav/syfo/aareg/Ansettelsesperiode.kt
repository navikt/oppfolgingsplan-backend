package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Ansettelsesperiode(
    var bruksperiode: Bruksperiode? = null,
    var periode: Periode,
    var sporingsinformasjon: Sporingsinformasjon? = null,
    var varslingskode: String? = null
) : Serializable
