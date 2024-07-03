package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Bruksperiode(
    var fom: String,
    var tom: String
) : Serializable
