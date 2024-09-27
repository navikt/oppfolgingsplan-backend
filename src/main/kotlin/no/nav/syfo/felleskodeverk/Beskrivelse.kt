package no.nav.syfo.felleskodeverk

import java.io.Serializable

data class Beskrivelse(
    var tekst: String? = null,
    var term: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
