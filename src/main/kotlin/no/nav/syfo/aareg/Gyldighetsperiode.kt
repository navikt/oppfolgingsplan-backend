package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Gyldighetsperiode(
    var fom: String,
    var tom: String
) : Serializable
