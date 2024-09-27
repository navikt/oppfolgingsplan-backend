package no.nav.syfo.felleskodeverk

import java.io.Serializable

data class Betydning(
    var beskrivelser: Map<String, Beskrivelse>? = null,
    var gyldigFra: String? = null,
    var gyldigTil: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
