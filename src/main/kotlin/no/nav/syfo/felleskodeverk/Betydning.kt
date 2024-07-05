package no.nav.syfo.felleskodeverk

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Betydning(
    var beskrivelser: Map<String, Beskrivelse>? = null,
    var gyldigFra: String? = null,
    var gyldigTil: String? = null
) : Serializable