package no.nav.syfo.felleskodeverk

import java.io.Serializable

data class KodeverkKoderBetydningerResponse(
    var betydninger: Map<String, List<Betydning>>? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
