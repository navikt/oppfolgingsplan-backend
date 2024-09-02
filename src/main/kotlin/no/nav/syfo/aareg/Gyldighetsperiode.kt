package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Gyldighetsperiode(
    var fom: String? = null,
    var tom: String? = null
) : Serializable
