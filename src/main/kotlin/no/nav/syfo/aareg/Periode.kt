package no.nav.syfo.aareg

import java.io.Serializable

data class Periode(
    var fom: String,
    var tom: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
