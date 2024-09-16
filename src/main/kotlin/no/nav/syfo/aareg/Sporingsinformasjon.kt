package no.nav.syfo.aareg

import java.io.Serializable

data class Sporingsinformasjon(
    var endretAv: String,
    var endretKilde: String,
    var endretKildereferanse: String,
    var endretTidspunkt: String,
    var opprettetAv: String,
    var opprettetKilde: String,
    var opprettetKildereferanse: String,
    var opprettetTidspunkt: String
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
