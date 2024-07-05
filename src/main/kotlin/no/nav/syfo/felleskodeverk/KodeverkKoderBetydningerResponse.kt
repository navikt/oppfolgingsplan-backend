package no.nav.syfo.felleskodeverk

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class KodeverkKoderBetydningerResponse(
    var betydninger: Map<String, List<Betydning>>? = null
) : Serializable
