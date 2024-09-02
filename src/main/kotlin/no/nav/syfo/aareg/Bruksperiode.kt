package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Bruksperiode(
    var fom: String? = null,
    var tom: String? = null
) : Serializable
