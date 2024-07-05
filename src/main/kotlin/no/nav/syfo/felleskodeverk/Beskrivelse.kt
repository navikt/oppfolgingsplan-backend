package no.nav.syfo.felleskodeverk

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Beskrivelse(
    var tekst: String? = null,
    var term: String? = null
) : Serializable
